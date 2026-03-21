package com.codraw.CoDraw.handler;

import com.codraw.CoDraw.model.StrokeMessage;
import com.codraw.CoDraw.service.CanvasStateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler ho tro phong ve.
 *
 * URL ket noi: ws://host:8080/ws/draw?roomCode=ABC123&token=<jwt>
 *
 * Tin hieu dac biet (JSON):
 *   {"type":"JOIN","roomCode":"ABC123"}   -> server gui lai lich su strokes tu Redis
 *   {"type":"CLEAR","roomCode":"ABC123"}  -> xoa canvas cua phong tren Redis
 *   {"type":"STROKE", ... stroke fields}  -> broadcast + luu vao Redis
 *
 * Fix: Wrap moi session bang ConcurrentWebSocketSessionDecorator de ghi message
 *      tu nhieu thread dong thoi ma khong bi loi "sendMessage from multiple threads".
 *      Send timeout 5s, buffer 256KB – du lon cho preview strokes.
 */
@Component
public class DrawingWebSocketHandler extends TextWebSocketHandler {

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

        // Wrap the session so concurrent sends are safe (queued, not thrown)
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
                System.err.println("[CoDraw] Error replaying history to " + rawSession.getId() + ": " + e.getMessage());
            }
        }

        System.out.println("[CoDraw] Session " + rawSession.getId()
                + " joined room " + roomCode
                + " | total in room: " + rooms.get(roomCode).size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession rawSession, TextMessage message) throws Exception {
        String roomCode = sessionRoom.get(rawSession.getId());
        if (roomCode == null) return;

        String payload = message.getPayload();
        @SuppressWarnings("unchecked")
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
            // Relay-only: forward to peers, do NOT persist to Redis
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
        System.out.println("[CoDraw] Session " + rawSession.getId()
                + " disconnected | status: " + status);
    }

    @Override
    public void handleTransportError(WebSocketSession rawSession, Throwable exception) {
        // Log but do NOT call afterConnectionClosed manually — Spring will call it
        System.err.println("[CoDraw] Transport error for session " + rawSession.getId()
                + ": " + exception.getMessage());
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
                    System.err.println("[CoDraw] Broadcast error to " + s.getId() + ": " + e.getMessage());
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
