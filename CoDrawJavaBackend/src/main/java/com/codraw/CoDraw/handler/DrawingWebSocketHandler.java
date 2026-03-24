package com.codraw.CoDraw.handler;

import com.codraw.CoDraw.model.StrokeMessage;
import com.codraw.CoDraw.service.CanvasStateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for drawing rooms.
 *
 * URL: ws://host:8080/ws/draw?roomCode=ABC123&token=<jwt>
 *
 * Wraps each session with {@link ConcurrentWebSocketSessionDecorator} to allow
 * safe concurrent message sends (queued, not thrown).
 * Send timeout 5 s, buffer 256 KB – sufficient for preview strokes.
 */
@Component
public class DrawingWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(DrawingWebSocketHandler.class);

    private static final int SEND_TIME_LIMIT_MS = 5_000;
    private static final int SEND_BUFFER_SIZE_LIMIT = 256 * 1024; // 256KB

    private static final Set<String> RELAY_ONLY_TYPES = Set.of(
            "STROKE_PREVIEW",
            "COMPLETE_REQUEST",
            "COMPLETE_RESPONSE",
            "COMPLETE_FINALIZED",
            "COMPLETE_CANCELLED",
            "VIEWPORT",
            "UNDO",
            "CHAT"
    );

    // roomCode -> Set<ConcurrentWebSocketSessionDecorator>
    private final Map<String, Set<ConcurrentWebSocketSessionDecorator>> rooms = new ConcurrentHashMap<>();
    // sessionId -> roomCode
    private final Map<String, String> sessionRoom = new ConcurrentHashMap<>();
    // sessionId -> ConcurrentWebSocketSessionDecorator (to retrieve for removal)
    private final Map<String, ConcurrentWebSocketSessionDecorator> sessionDecorators = new ConcurrentHashMap<>();

    private final CanvasStateService canvasStateService;
    private final ObjectMapper objectMapper;

    public DrawingWebSocketHandler(CanvasStateService canvasStateService,
                                    ObjectMapper objectMapper) {
        this.canvasStateService = canvasStateService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession rawSession) {
        String roomCode = getRoomCode(rawSession);
        if (roomCode == null || roomCode.isBlank()) return;

        ConcurrentWebSocketSessionDecorator session = new ConcurrentWebSocketSessionDecorator(
                rawSession, SEND_TIME_LIMIT_MS, SEND_BUFFER_SIZE_LIMIT
        );

        rooms.computeIfAbsent(roomCode, k -> ConcurrentHashMap.newKeySet()).add(session);
        sessionRoom.put(rawSession.getId(), roomCode);
        sessionDecorators.put(rawSession.getId(), session);

        // Replay stroke history for the newly joined player
        List<StrokeMessage> history = canvasStateService.getStrokes(roomCode);
        for (StrokeMessage stroke : history) {
            try {
                String json = objectMapper.writeValueAsString(stroke);
                session.sendMessage(new TextMessage(json));
            } catch (Exception e) {
                log.error("Error replaying history to session {}: {}", rawSession.getId(), e.getMessage());
            }
        }

        log.info("Session {} joined room {} | total in room: {}",
                rawSession.getId(), roomCode, rooms.get(roomCode).size());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void handleTextMessage(WebSocketSession rawSession, TextMessage message) throws Exception {
        String roomCode = sessionRoom.get(rawSession.getId());
        if (roomCode == null) return;

        String payload = message.getPayload();
        Map<String, Object> map = objectMapper.readValue(payload, Map.class);
        String type = map.getOrDefault("type", "STROKE").toString();

        if ("JOIN".equals(type)) {
            return; // Already handled in afterConnectionEstablished
        }

        if ("CLEAR".equals(type)) {
            canvasStateService.clearStrokes(roomCode);
            broadcast(roomCode, rawSession.getId(), payload, false);
            return;
        }

        if (RELAY_ONLY_TYPES.contains(type)) {
            broadcast(roomCode, rawSession.getId(), payload, true);
            return;
        }

        // STROKE default: save to Redis and broadcast to peers
        StrokeMessage stroke = objectMapper.readValue(payload, StrokeMessage.class);
        stroke.setType(type);
        stroke.setPreview(false);
        canvasStateService.addStroke(roomCode, stroke);
        broadcast(roomCode, rawSession.getId(), objectMapper.writeValueAsString(stroke), true);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession rawSession, CloseStatus status) {
        removeSession(rawSession.getId());
        log.info("Session {} disconnected | status: {}", rawSession.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession rawSession, Throwable exception) {
        log.error("Transport error for session {}: {}", rawSession.getId(), exception.getMessage());
        try {
            if (rawSession.isOpen()) rawSession.close(CloseStatus.SERVER_ERROR);
        } catch (Exception ignored) {}
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void removeSession(String sessionId) {
        String roomCode = sessionRoom.remove(sessionId);
        ConcurrentWebSocketSessionDecorator decorator = sessionDecorators.remove(sessionId);
        if (roomCode != null && decorator != null) {
            Set<ConcurrentWebSocketSessionDecorator> roomSessions = rooms.get(roomCode);
            if (roomSessions != null) {
                roomSessions.remove(decorator);
                if (roomSessions.isEmpty()) rooms.remove(roomCode);
            }
        }
    }

    private void broadcast(String roomCode, String senderId,
                           String payload, boolean excludeSender) {
        Set<ConcurrentWebSocketSessionDecorator> roomSessions = rooms.get(roomCode);
        if (roomSessions == null) return;

        TextMessage msg = new TextMessage(payload);
        for (ConcurrentWebSocketSessionDecorator s : roomSessions) {
            if (excludeSender && s.getId().equals(senderId)) continue;
            if (s.isOpen()) {
                try {
                    s.sendMessage(msg);
                } catch (Exception e) {
                    log.warn("Broadcast error to session {}: {}", s.getId(), e.getMessage());
                }
            }
        }
    }

    private String getRoomCode(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=");
            if (kv.length == 2 && "roomCode".equals(kv[0])) return kv[1].toUpperCase();
        }
        return null;
    }
}
