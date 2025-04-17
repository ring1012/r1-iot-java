package huan.diy.r1iot.service.box;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class BoxControllerService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean control(String clientIp, String target, String action) {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            URI uri = new URI("ws://" + clientIp + ":8080");
            WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory
                    .newHandshaker(uri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders());

            // 使用CompletableFuture来接收服务端响应
            CompletableFuture<String> responseFuture = new CompletableFuture<>();

            WebSocketClientHandler handler = new WebSocketClientHandler(handshaker, responseFuture);

            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new HttpClientCodec());
                            pipeline.addLast(new HttpObjectAggregator(8192));
                            pipeline.addLast(handler);
                        }
                    });

            ChannelFuture future = bootstrap.connect(uri.getHost(), uri.getPort());
            if (!future.await(800, TimeUnit.MILLISECONDS)) {
                log.info("连接超时");
                return false;
            }

            if (!future.isSuccess()) {
                log.info("连接失败: " + future.cause());
                return false;
            }

            new Thread(() -> {
                try {
                    future.channel().closeFuture().sync();
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }).start();

            Channel channel = future.channel();
            handler.handshakeFuture().sync();

            // 第一步：发送get_info请求
            String getInfoJson = buildGetInfoJson();
            channel.writeAndFlush(new TextWebSocketFrame(getInfoJson));
//            log.info("已发送get_info请求: {}", getInfoJson);

            // 等待服务端返回信息（最多2秒）
            String response = responseFuture.get(200, TimeUnit.MILLISECONDS);
//            log.info("收到服务端响应: {}", response);

            JsonNode infoNode = objectMapper.readTree(response);
            if (infoNode.get("code").asInt() == 403) {
                String login = "{\"type\":\"send_message\",\"what\":65536,\"arg1\":3,\"arg2\":0,\"obj\":\"{\\\"type\\\":\\\"login_state\\\",\\\"code\\\":1,\\\"login_state\\\":\\\"1\\\"}\"}";
                channel.writeAndFlush(new TextWebSocketFrame(login));
                channel.writeAndFlush(new TextWebSocketFrame(getInfoJson));
                response = responseFuture.get(200, TimeUnit.MILLISECONDS);

            }



            // 第二步：根据响应构建并发送实际请求
            String requestJson = buildRequestBasedOnResponse(response, target, action);


            if (!StringUtils.hasLength(requestJson)) {
                return false;
            }
            channel.writeAndFlush(new TextWebSocketFrame(requestJson));
//            log.info("已发送实际请求: {}", requestJson);

            // 等待短暂时间确保请求发送完成
            TimeUnit.MILLISECONDS.sleep(200);

            // 主动关闭连接
            channel.writeAndFlush(new CloseWebSocketFrame());
            channel.closeFuture().sync();


            return true;


        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        } finally {
            group.shutdownGracefully();
        }
    }

    // 构建get_info请求JSON
    private String buildGetInfoJson() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "get_info");
        return objectMapper.writeValueAsString(map);
    }

    // 根据响应构建实际请求
    private String buildRequestBasedOnResponse(String response, String target, String action) throws Exception {

        JsonNode respNode = objectMapper.readTree(response);
        String data = respNode.get("data").asText();
        JsonNode dataNode = objectMapper.readTree(data);


        return switch (target) {
            case "lamp" -> buildLampRequest(dataNode, action);
            case "faster" -> adjustTime(dataNode, action, "faster");
            case "slower" -> adjustTime(dataNode, action, "slower");
            case "jump" -> adjustTime(dataNode, action, "jump");
            default -> "";
        };
    }

    private String adjustTime(JsonNode dataNode, String action, String faster) {
        int duration = dataNode.get("music_info").get("duration").asInt();
        int position = dataNode.get("music_info").get("position").asInt();

        int window = 10 * 1000;
        if (StringUtils.hasLength(action)) {
            window = Integer.parseInt(action) * 1000;
        }

        int newPosition = switch (faster) {
            case "faster" -> position + window;
            case "slower" -> position - window;
            case "jump" -> Integer.parseInt(action) * 1000;
            default -> position;
        };

        newPosition = Math.max(0, Math.min(newPosition, duration));

        ObjectNode ret = objectMapper.createObjectNode();
        ret.put("type", "set_position");
        ret.put("position", newPosition);
        return ret.toString();

    }

    private String buildLampRequest(JsonNode dataNode, String action) {

        if ("change".equalsIgnoreCase(action)) {
            int arg2 = dataNode.get("music_light_mode").asInt();
            if (arg2 == 4) {
                arg2 = 0;
            } else {
                arg2++;
            }
            ObjectNode ret = objectMapper.createObjectNode();
            ret.put("what", 4);
            ret.put("arg1", 68);
            ret.put("arg2", arg2);
            ret.put("type", "send_message");
            return ret.toString();
        }

        int onArg = "off".equalsIgnoreCase(action) ? 0 : 1;

        ObjectNode ret = objectMapper.createObjectNode();
        ret.put("what", 4);
        ret.put("arg1", 64);
        ret.put("arg2", onArg);
        ret.put("type", "send_message");
        return ret.toString();
    }



    // 修改后的WebSocketClientHandler，支持响应回调
    private static class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {
        private final WebSocketClientHandshaker handshaker;
        private ChannelPromise handshakeFuture;
        private final CompletableFuture<String> responseFuture;

        public WebSocketClientHandler(WebSocketClientHandshaker handshaker, CompletableFuture<String> responseFuture) {
            this.handshaker = handshaker;
            this.responseFuture = responseFuture;
        }

        public ChannelFuture handshakeFuture() {
            return handshakeFuture;
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            handshakeFuture = ctx.newPromise();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            handshaker.handshake(ctx.channel());
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            Channel ch = ctx.channel();
            if (!handshaker.isHandshakeComplete()) {
                handshaker.finishHandshake(ch, (FullHttpResponse) msg);
                handshakeFuture.setSuccess();
                return;
            }

            if (msg instanceof TextWebSocketFrame) {
                String text = ((TextWebSocketFrame) msg).text();
//                log.info("收到服务端消息: " + text);
                // 将响应传递给CompletableFuture
                if (!responseFuture.isDone()) {
                    responseFuture.complete(text);
                }
            } else if (msg instanceof CloseWebSocketFrame) {
                ch.close();
                if (!responseFuture.isDone()) {
                    responseFuture.completeExceptionally(new RuntimeException("Connection closed by server"));
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            if (!handshakeFuture.isDone()) {
                handshakeFuture.setFailure(cause);
            }
            if (!responseFuture.isDone()) {
                responseFuture.completeExceptionally(cause);
            }
            ctx.close();
        }
    }
}