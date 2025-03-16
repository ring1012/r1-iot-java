package huan.diy.r1iot.configure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import huan.diy.r1iot.helper.AsrServerHandler;
import huan.diy.r1iot.model.AsrHandleType;
import huan.diy.r1iot.model.AsrResult;
import huan.diy.r1iot.service.AiFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
public class TcpServerController {
    private static final Logger logger = LoggerFactory.getLogger(TcpServerController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    // 远程服务器地址
    private static final String REMOTE_HOST = "47.102.50.144";  // 目标服务器 IP
    private static final int REMOTE_PORT = 80;  // 远程服务器的端口
    private final Bootstrap remoteBootstrap = new Bootstrap();

    @Autowired
    private AsrServerHandler asrServerHandler;

    @Autowired
    private AiFactory aiFactory;

    @PostConstruct
    public void startTcpServer() {
        int port = 80;  // 服务器监听端口

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        // 初始化远程服务器的 Bootstrap
        remoteBootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new IdleStateHandler(0, 0, 30, TimeUnit.SECONDS));
                        ch.pipeline().addLast(new RemoteServerHandler());
                    }
                });

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new TcpForwardHandler());
                        }
                    });

            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();
            logger.info("TCP Server started on port {}", port);

            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            logger.error("TCP Server interrupted", e);
            throw new RuntimeException(e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    // 自定义 TCP 转发处理器
    private class TcpForwardHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof ByteBuf) {
                ByteBuf data = (ByteBuf) msg;
                forwardToRemoteServer(ctx, data);
            } else {
                logger.error("Received unknown message type: {}", msg.getClass());
                ctx.close();
            }
        }

        private void forwardToRemoteServer(ChannelHandlerContext ctx, ByteBuf data) {
            Channel remoteChannel = ctx.channel().attr(ChannelAttributes.REMOTE_CHANNEL).get();
            if (remoteChannel != null && remoteChannel.isActive()) {
                remoteChannel.writeAndFlush(data.retain());
                return;
            }

            ChannelFuture future = remoteBootstrap.connect(new InetSocketAddress(REMOTE_HOST, REMOTE_PORT));
            future.addListener((ChannelFutureListener) f -> {
                if (f.isSuccess()) {
                    Channel newRemoteChannel = f.channel();
                    ctx.channel().attr(ChannelAttributes.REMOTE_CHANNEL).set(newRemoteChannel);
                    newRemoteChannel.attr(ChannelAttributes.CLIENT_CHANNEL).set(ctx.channel());
                    newRemoteChannel.writeAndFlush(data.retain());

                    newRemoteChannel.closeFuture().addListener((ChannelFutureListener) closeFuture -> {
                        ctx.channel().attr(ChannelAttributes.REMOTE_CHANNEL).set(null);
                        logger.info("Remote server connection closed");
                    });
                } else {
                    logger.error("Failed to connect to remote server: {}", f.cause().getMessage());
                    ctx.close();
                }
            });
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("TCP Server error", cause);
            ctx.close();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            Channel remoteChannel = ctx.channel().attr(ChannelAttributes.REMOTE_CHANNEL).getAndSet(null);
            if (remoteChannel != null) {
                remoteChannel.close();
            }
            logger.info("Client disconnected, remote channel closed");
        }
    }

    // 远程服务器处理器
    // 远程服务器处理器
    private class RemoteServerHandler extends ChannelInboundHandlerAdapter {
        private StringBuffer accumulatedData = new StringBuffer();

        public synchronized void appendData(String data) {
            accumulatedData.append(data);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof ByteBuf) {
                ByteBuf responseData = (ByteBuf) msg;
                String data = responseData.toString(StandardCharsets.UTF_8);
                logger.info("each data from remote server: {}", data);

                AsrResult asrResult = asrServerHandler.handle(data);
                Channel clientChannel = ctx.channel().attr(ChannelAttributes.CLIENT_CHANNEL).get();
                if (clientChannel == null) {
                    return;
                }
                switch (asrResult.getType()) {
                    case DROPPED, SKIP:
                        clientChannel.writeAndFlush(ctx.alloc().buffer().writeBytes(data.getBytes()));
                        return;
                    case APPEND:
                        appendData(asrResult.getFixedData());
                        try {
                            String[] lines = accumulatedData.toString().split("\n");
                            JsonNode node = objectMapper.readTree(lines[lines.length - 1]);
                            if (node.has("responseId")) {
                                break;
                            }
                            return;
                        } catch (Exception e) {
                            return;
                        }
                    case END:
                        appendData(asrResult.getFixedData());
                        break;

                }
                logger.info("from R1: {}", accumulatedData.toString());
                String aiReply = asrServerHandler.enhance(accumulatedData.toString());
                logger.info("from AI: {}", aiReply);
                if (aiReply == null) {
                    clientChannel.writeAndFlush(ctx.alloc().buffer().writeBytes(accumulatedData.toString().getBytes()));
                    return;
                }

                clientChannel.writeAndFlush(ctx.alloc().buffer().writeBytes(aiReply.getBytes()));
                accumulatedData.setLength(0);  // 清空累积的数据
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (evt instanceof IdleStateEvent) {
                logger.info("Sending heartbeat to remote server");
                ctx.writeAndFlush(ctx.alloc().buffer().writeBytes("HEARTBEAT".getBytes()));
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("Remote server handler error", cause);
            ctx.close();
        }
    }

    // Channel 属性键
    private static class ChannelAttributes {
        private static final AttributeKey<Channel> REMOTE_CHANNEL = AttributeKey.valueOf("REMOTE_CHANNEL");
        private static final AttributeKey<Channel> CLIENT_CHANNEL = AttributeKey.valueOf("CLIENT_CHANNEL");
    }
}