package com.codraw.CoDraw.handler;

import com.codraw.CoDraw.security.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GlobalWebSocketHandler extends TextWebSocketHandler {

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    // username -> Session Decorator
    private final Map<String, ConcurrentWebSocketSessionDecorator> userSessions = new ConcurrentHashMap<>();
    // sessionId -> username
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();

    private static final int SEND_TIME_LIMIT_MS = 5_000;
    private static final int SEND_BUFFER_SIZE_LIMIT = 512 * 1024;

    public GlobalWebSocketHandler(JwtUtils jwtUtils, ObjectMapper objectMapper) {
        this.jwtUtils = jwtUtils;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession rawSession) throws Exception {
        String token = getToken(rawSession);
        if (token == null || !jwtUtils.validateToken(token)) {
            rawSession.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid token"));
            return;
        }

        String username = jwtUtils.getUsernameFromToken(token);
        
        ConcurrentWebSocketSessionDecorator session = new ConcurrentWebSocketSessionDecorator(
                rawSession, SEND_TIME_LIMIT_MS, SEND_BUFFER_SIZE_LIMIT
        );

        userSessions.put(username, session);
        sessionUserMap.put(rawSession.getId(), username);

        System.out.println("[CoDraw] Global Session " + rawSession.getId() + " connected for user " + username);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession rawSession, CloseStatus status) {
        String username = sessionUserMap.remove(rawSession.getId());
        if (username != null) {
            userSessions.remove(username);
            System.out.println("[CoDraw] Global Session " + rawSession.getId() + " disconnected, user " + username);
        }
    }

    public void pushToUser(String targetUsername, Object payload) {
        ConcurrentWebSocketSessionDecorator session = userSessions.get(targetUsername);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
            } catch (Exception e) {
                System.err.println("Failed to push message to " + targetUsername + ": " + e.getMessage());
            }
        }
    }

    private String getToken(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=");
            if (kv.length == 2 && "token".equals(kv[0])) return kv[1];
        }
        return null;
    }
}
