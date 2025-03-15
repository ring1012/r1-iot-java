package huan.diy.r1iot.configure;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class MyWebSocketHandler extends TextWebSocketHandler {

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 处理客户端发送的消息
        String clientMessage = message.getPayload();
        System.out.println("Received message: " + clientMessage);

        // 发送 "success" 作为 ACK
        session.sendMessage(new TextMessage("success"));
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 当客户端连接成功时调用
        System.out.println("Client connected: " + session.getId());
        session.sendMessage(new TextMessage("success")); // 发送 "success" 作为 ACK
    }
}