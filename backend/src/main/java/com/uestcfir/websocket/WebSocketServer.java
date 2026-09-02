package com.uestcfir.websocket;

import com.uestcfir.pojo.entity.Reservations;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket服务
 */
@Component
@Slf4j
@ServerEndpoint("/ws/{sid}")
public class WebSocketServer {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    // 存放会话对象
    private static Map<String, Session> sessionMap = new HashMap<>();

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        log.info("连接成功:" + sid);
        sessionMap.put(sid, session);
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        log.info("收到消息:" + message);
    }

    /**
     * 连接关闭调用的方法
     *
     * @param sid
     */
    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        log.info("连接关闭:" + sid);
        sessionMap.remove(sid);
    }

    /**
     * 群发
     *
     * @param message
     */
    public void sendToAllClient(String message) {
        Collection<Session> sessions = sessionMap.values();
        for (Session session : sessions) {
            try {
                // 服务器向客户端发送消息
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMeetingReminder(Integer userId, Reservations meeting) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "MEETING_REMINDER");
        message.put("meetingId", meeting.getReservationId());
        message.put("date", meeting.getReserveDate().toString());
        message.put("startTime", meeting.getStartTime().toString());
        message.put("room", meeting.getRoomId());
        message.put("message", String.format("您预约的会议即将在5分钟后开始"));

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/meeting-reminders",
                message
        );

        log.info("已向用户 {} 发送会议提醒: {}", userId, meeting.getReservationId());
    }
}
