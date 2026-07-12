package com.bank.web.websocket;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@ServerEndpoint("/notifications/{userId}")
public class NotificationWebSocket {

    private static final Logger logger = Logger.getLogger(NotificationWebSocket.class.getName());
    
    // Concurrent map to keep track of active sessions per user
    private static final Map<Long, Session> userSessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        userSessions.put(userId, session);
        logger.info("WebSocket connected for user ID: " + userId);
    }

    @OnClose
    public void onClose(Session session, @PathParam("userId") Long userId) {
        userSessions.remove(userId);
        logger.info("WebSocket disconnected for user ID: " + userId);
    }

    @OnError
    public void onError(Session session, Throwable throwable, @PathParam("userId") Long userId) {
        userSessions.remove(userId);
        logger.severe("WebSocket error for user ID: " + userId + " - " + throwable.getMessage());
    }

    public static void sendNotification(Long userId, String message) {
        Session session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
                logger.info("Sent real-time notification to user ID: " + userId);
            } catch (IOException e) {
                logger.severe("Failed to send WebSocket notification to user ID: " + userId + " - " + e.getMessage());
            }
        }
    }
}
