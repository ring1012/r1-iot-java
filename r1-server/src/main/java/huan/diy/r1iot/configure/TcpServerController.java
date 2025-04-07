package huan.diy.r1iot.configure;

import huan.diy.r1iot.helper.AsrServerHandler;
import huan.diy.r1iot.helper.KeepClientTCPAlive;
import huan.diy.r1iot.helper.RemoteServerHandler;
import huan.diy.r1iot.helper.TcpForwardHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class TcpServerController {
    private final Bootstrap remoteBootstrap = new Bootstrap();

    @Autowired
    private AsrServerHandler asrServerHandler;

    @Autowired
    private KeepClientTCPAlive keepClientTCPAlive;


    public void initTcp() {
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
                        ch.pipeline().addLast(new RemoteServerHandler(asrServerHandler, keepClientTCPAlive));
                    }
                });

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new TcpForwardHandler(remoteBootstrap));
                        }
                    });

            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();
            log.info("TCP Server started on port {}", port);

            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("TCP Server interrupted", e);
            throw new RuntimeException(e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    @PostConstruct
    public void startTcpServer() {
        new Thread(this::initTcp).start();

    }


}