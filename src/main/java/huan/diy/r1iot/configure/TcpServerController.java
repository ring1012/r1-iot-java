package huan.diy.r1iot.configure;

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
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Component
public class TcpServerController {
    private static final Logger logger = LoggerFactory.getLogger(TcpServerController.class);

    // 远程服务器地址
    private static final String REMOTE_HOST = "47.102.50.144";  // 目标服务器 IP
    private static final int REMOTE_PORT = 80;  // 远程服务器的端口

    // 复用 Bootstrap 和连接池
    private final Bootstrap remoteBootstrap = new Bootstrap();
    private final ConcurrentMap<ChannelHandlerContext, Channel> clientToRemoteChannelMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void startTcpServer() {
        // 服务器监听端口
        int port = 80;  // 可以改成需要的端口

        // 创建两个 EventLoopGroup，一个用于接收连接，一个用于处理连接
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        // 初始化远程服务器的 Bootstrap
        remoteBootstrap.group(workerGroup)  // 复用 workerGroup
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        // 添加心跳机制
                        ch.pipeline().addLast(new IdleStateHandler(0, 0, 30, TimeUnit.SECONDS));  // 30秒空闲检测
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                // 接收远程服务器的 TCP 数据并返回给客户端
                                if (msg instanceof ByteBuf) {
                                    ByteBuf responseData = (ByteBuf) msg;
                                    logger.info("Received TCP data from remote server: {}", responseData.toString(java.nio.charset.StandardCharsets.UTF_8));

                                    // 找到对应的客户端 Channel 并返回数据
                                    Channel clientChannel = clientToRemoteChannelMap.get(ctx);
                                    if (clientChannel != null) {
                                        clientChannel.writeAndFlush(responseData.retain());
                                    }
                                }
                            }

                            @Override
                            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
                                // 处理空闲事件，发送心跳包
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
                        });
                    }
                });

        try {
            // 创建 ServerBootstrap
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)  // 使用 NIO 传输
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // 直接处理字节流，不涉及 HTTP 协议
                            ch.pipeline().addLast(new TcpForwardHandler());
                        }
                    });

            // 绑定端口并启动服务器
            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();
            logger.info("TCP Server started on port {}", port);

            // 等待服务器关闭
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            logger.error("TCP Server interrupted", e);
            throw new RuntimeException(e);
        } finally {
            // 优雅关闭
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    // 自定义 TCP 转发处理器
    private class TcpForwardHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            // 接收客户端的 TCP 数据
            if (msg instanceof ByteBuf) {
                ByteBuf data = (ByteBuf) msg;
                logger.info("Received TCP data from client: {}", data.toString(StandardCharsets.UTF_8));

                // 将数据转发到远程服务器
                forwardToRemoteServer(ctx, data);
            } else {
                logger.error("Received unknown message type: {}", msg.getClass());
                ctx.close();
            }
        }

        private void forwardToRemoteServer(ChannelHandlerContext ctx, ByteBuf data) {
            // 检查是否已经有到远程服务器的连接
            Channel remoteChannel = clientToRemoteChannelMap.get(ctx);
            if (remoteChannel != null && remoteChannel.isActive()) {
                // 如果连接已存在且活跃，直接发送数据
                remoteChannel.writeAndFlush(data.retain());
                return;
            }

            // 如果没有连接，创建新的连接
            ChannelFuture future = remoteBootstrap.connect(new InetSocketAddress(REMOTE_HOST, REMOTE_PORT));
            future.addListener((ChannelFutureListener) f -> {
                if (f.isSuccess()) {
                    // 连接成功，发送数据
                    Channel newRemoteChannel = f.channel();
                    clientToRemoteChannelMap.put(ctx, newRemoteChannel);  // 保存客户端与远程服务器的映射
                    newRemoteChannel.writeAndFlush(data.retain());

                    // 监听远程 Channel 的关闭事件
                    newRemoteChannel.closeFuture().addListener((ChannelFutureListener) closeFuture -> {
                        clientToRemoteChannelMap.remove(ctx);  // 移除映射
                        logger.info("Remote server connection closed");
                    });
                } else {
                    // 连接失败
                    logger.error("Failed to connect to remote server: {}", f.cause().getMessage());
                    ctx.close();
                }
            });
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            // 异常处理
            logger.error("TCP Server error", cause);
            ctx.close();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            // 客户端断开连接时，关闭对应的远程服务器连接
            Channel remoteChannel = clientToRemoteChannelMap.remove(ctx);
            if (remoteChannel != null) {
                remoteChannel.close();
            }
            logger.info("Client disconnected, remote channel closed");
        }
    }
}