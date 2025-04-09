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
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class TcpServerController {
    private final Bootstrap remoteBootstrap = new Bootstrap();
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @Autowired
    private AsrServerHandler asrServerHandler;

    @Autowired
    private KeepClientTCPAlive keepClientTCPAlive;

    public void initTcp() throws InterruptedException {
        int port = 80;  // 服务器监听端口

        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();

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

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        ChannelFuture bindFuture = serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new TcpForwardHandler(remoteBootstrap));
                    }
                })
                .bind(port);

        // 添加监听器异步处理结果
        bindFuture.addListener(future -> {
            if (future.isSuccess()) {
                log.info("TCP Server started successfully on port {}", port);
            } else {
                log.error("Failed to start TCP Server on port {}", port, future.cause());
                // 关闭资源
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
                throw new RuntimeException("Failed to start TCP Server", future.cause());
            }
        });

        // 同步等待绑定结果（但不等待关闭）
        bindFuture.sync();
    }

    @PostConstruct
    public void startTcpServer() {
        try {
            this.initTcp();
        } catch (InterruptedException e) {
            log.error("TCP Server initialization interrupted", e);
            throw new RuntimeException("TCP Server initialization failed", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }
}
