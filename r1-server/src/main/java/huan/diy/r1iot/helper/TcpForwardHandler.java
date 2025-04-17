package huan.diy.r1iot.helper;

import huan.diy.r1iot.util.TcpChannelUtils;
import huan.diy.r1iot.util.R1IotUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

@Slf4j
public class TcpForwardHandler extends ChannelInboundHandlerAdapter {

    private final Bootstrap remoteBootstrap;

    public TcpForwardHandler(Bootstrap remoteBootstrap) {
        super();
        this.remoteBootstrap = remoteBootstrap;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof ByteBuf) {
            ByteBuf data = (ByteBuf) msg;
            forwardToRemoteServer(ctx, data);
        } else {
            log.error("Received unknown message type: {}", msg.getClass());
            ctx.close();
        }
    }

    private void forwardToRemoteServer(ChannelHandlerContext ctx, ByteBuf data) {
        String body = data.toString(StandardCharsets.ISO_8859_1);
        if (body.contains("TP:") && !body.contains("UI:")) {
            ctx.channel().attr(TcpChannelUtils.END).set(true);
        }

        SocketAddress socketAddress = ctx.channel().remoteAddress();
        if (socketAddress instanceof InetSocketAddress) {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
            String clientIp = inetSocketAddress.getAddress().getHostAddress();
            ctx.channel().attr(TcpChannelUtils.CLIENT_IP).set(clientIp);
        }
//        log.info("from client: {}", body);
//            new Thread(new PCMDataAggregator(data.toString(StandardCharsets.ISO_8859_1).getBytes(StandardCharsets.ISO_8859_1))).start();
        String deviceId = setupCurrentDevice(data.toString(StandardCharsets.ISO_8859_1));
        if (StringUtils.hasLength(deviceId)) {
            ctx.channel().attr(TcpChannelUtils.DEVICE_ID).set(deviceId);
        }
        Channel remoteChannel = ctx.channel().attr(TcpChannelUtils.REMOTE_CHANNEL).get();
        if (remoteChannel != null && StringUtils.hasLength(deviceId)) {
            remoteChannel.attr(TcpChannelUtils.DEVICE_ID).set(deviceId);
        }

        if (remoteChannel != null && remoteChannel.isActive()) {
            remoteChannel.writeAndFlush(data.retain());
            return;
        }

        ChannelFuture future = remoteBootstrap.connect(new InetSocketAddress(TcpChannelUtils.REMOTE_HOST, TcpChannelUtils.REMOTE_PORT));
        future.addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                Channel newRemoteChannel = f.channel();
                ctx.channel().attr(TcpChannelUtils.REMOTE_CHANNEL).set(newRemoteChannel);
                newRemoteChannel.attr(TcpChannelUtils.CLIENT_CHANNEL).set(ctx.channel());
                newRemoteChannel.attr(TcpChannelUtils.DEVICE_ID).set(deviceId);
                newRemoteChannel.writeAndFlush(data.retain());

                newRemoteChannel.closeFuture().addListener((ChannelFutureListener) closeFuture -> {
                    ctx.channel().attr(TcpChannelUtils.REMOTE_CHANNEL).set(null);
                    log.info("Remote server connection closed");
                });
            } else {
                log.error("Failed to connect to remote server: {}", f.cause().getMessage());
                ctx.close();
            }
        });

    }

    private String setupCurrentDevice(String r1Input) {
        if (!r1Input.contains("Content-Length:0")) {
            return null;
        }
        String[] lines = r1Input.trim().split("\n");
        String[] infos = lines[lines.length - 1].trim().split("UI:");
        if (infos.length != 2) {
            return null;
        }
        String deviceId = infos[1];
        R1IotUtils.setCurrentDeviceId(deviceId);
        return deviceId;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("TCP Server error", cause);
//        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Channel remoteChannel = ctx.channel().attr(TcpChannelUtils.REMOTE_CHANNEL).getAndSet(null);
        if (remoteChannel != null) {
            remoteChannel.close();
        }
        log.info("Client disconnected, remote channel closed");
    }


    public class PCMDataAggregator implements Runnable {
        private static final Queue<byte[]> audioQueue = new LinkedList<>(); // Queue to store PCM data

        private final byte[] data;

        public PCMDataAggregator(byte[] data) {
            log.info("from client: {}", new String(data, StandardCharsets.ISO_8859_1)); // Optional logging for debugging
            this.data = data;
        }

        @Override
        public void run() {
            // Add incoming PCM data to the queue
            audioQueue.offer(data);

            // Start a timer to process data after TIMEOUT seconds
            try {
                writeToFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Method to handle file writing
        private void writeToFile() throws IOException {
            // Create a unique filename with the current timestamp
            File outputFile = new File("samples/" + new Date().getTime() + ".raw");

            // Ensure parent directories exist
            if (!outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }

            // Write data to file (append mode)
            try (FileOutputStream fos = new FileOutputStream(outputFile, true)) {
                fos.write(data);  // Write the byte[] directly to the file
                fos.flush();       // Ensure data is written to disk
            }
        }

    }
}

