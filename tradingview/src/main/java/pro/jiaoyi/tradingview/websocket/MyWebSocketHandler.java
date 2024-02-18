package pro.jiaoyi.tradingview.websocket;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Component
@Slf4j
public class MyWebSocketHandler implements WebSocketHandler {

    private List<WebSocketSession> sessions = new ArrayList<>();

    public int sessionCount() {
        return sessions.size();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 当WebSocket连接建立后调用此方法
        log.info("WebSocket连接已建立, session: {}", session);
        sessions.add(session);
    }


    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        // 当接收到WebSocket消息时调用此方法
        log.info("接收到消息：" + message.getPayload());

        // 处理消息逻辑...

        // 发送消息给客户端
    }

    public void send(String topic, String data) {
        // 发送消息给所有客户端
        JSONObject message = new JSONObject();
        message.put("topic", topic);
        message.put("data", data);
        for (WebSocketSession clientSession : sessions) {
            try {
                clientSession.sendMessage(new TextMessage(message.toJSONString()));
            } catch (IOException e) {
                log.warn("发送消息给客户端失败", e);
            }
        }

    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        // 当WebSocket传输发生错误时调用此方法
        log.error("WebSocket传输错误：" + exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        // 当WebSocket连接关闭后调用此方法
        log.info("WebSocket连接 remove, session: {}", session);
        sessions.remove(session);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}