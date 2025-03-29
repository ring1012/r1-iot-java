package huan.diy.r1iot.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import huan.diy.r1iot.model.AsrResult;
import huan.diy.r1iot.util.TcpChannelUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;


@Slf4j
public class RemoteServerHandler extends ChannelInboundHandlerAdapter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AsrServerHandler asrServerHandler;

    public RemoteServerHandler(AsrServerHandler asrServerHandler) {
        super();
        this.asrServerHandler = asrServerHandler;
    }


    private StringBuffer accumulatedData = new StringBuffer();

    public synchronized void appendData(String data) {
        accumulatedData.append(data);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof ByteBuf) {
            ByteBuf responseData = (ByteBuf) msg;
            String data = responseData.toString(StandardCharsets.UTF_8);
//                log.info("each data from remote server: {}", data);

            AsrResult asrResult = asrServerHandler.handle(data);
            Channel clientChannel = ctx.channel().attr(TcpChannelUtils.CLIENT_CHANNEL).get();
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
            log.info("from R1: {}", accumulatedData.toString());
            String aiReply = asrServerHandler.enhance(accumulatedData.toString(),
                    ctx.channel().attr(TcpChannelUtils.DEVICE_ID).get());

            log.info("from AI: {}", aiReply);
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
            log.info("Sending heartbeat to remote server");
            ctx.writeAndFlush(ctx.alloc().buffer().writeBytes("HEARTBEAT".getBytes()));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Remote server handler error", cause);
        ctx.close();
    }
}