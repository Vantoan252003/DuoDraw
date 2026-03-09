package com.codraw.CoDraw.handler;

import com.codraw.CoDraw.model.StrokeMessage;
import com.codraw.CoDraw.service.CanvasStateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

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
 */
@Component
public class DrawingWebSocketHandler extends TextWebSocketHandler {

    private static final Set<String> RELAY_ONLY_TYPES = Set.of(
            "STROKE_PREVIEW",
            "COMPLETE_REQUEST",
            "COMPLETE_RESPONSE",
            "COMPLETE_FINALIZED",
            "COMPLETE_CANCELLED"
    );

    // roomCode -> Set<session>
    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    // sessionId -> roomCode
    private final Map<String, String> sessionRoom = new ConcurrentHashMap<>();

    private final CanvasStateService canvasStateService;
    private final ObjectMapper objectMapper;

    public DrawingWebSocketHandler(CanvasStateService canvasStateService,
                                    ObjectMapper objectMapper) {
        this.canvasStateService = canvasStateService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // roomCode duoc truyen qua query param: ws://...?roomCode=ABC123
        String roomCode = getRoomCode(session);
        if (roomCode == null || roomCode.isBlank()) return;

        rooms.computeIfAbsent(roomCode, k -> ConcurrentHashMap.newKeySet()).add(session);
        sessionRoom.put(session.getId(), roomCode);

        // Gui lai toan bo lich su strokes tu Redis cho nguoi moi vao
        List<StrokeMessage> history = canvasStateService.getStrokes(roomCode);
        for (StrokeMessage stroke : history) {
            try {
                String json = objectMapper.writeValueAsString(stroke);
                session.sendMessage(new TextMessage(json));
            } catch (Exception ignored) {}
        }

        System.out.println("[CoDraw] Session " + session.getId()
                + " joined room " + roomCode
                + " | total in room: " + rooms.get(roomCode).size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String roomCode = sessionRoom.get(session.getId());
        if (roomCode == null) return;

        String payload = message.getPayload();
        @SuppressWarnings("unchecked")
        Map<String, Object> map = objectMapper.readValue(payload, Map.class);
        String type = map.getOrDefault("type", "STROKE").toString();

        if ("JOIN".equals(type)) {
            // Da xu ly trong afterConnectionEstablished, bo qua
            return;
        }

        if ("CLEAR".equals(type)) {
            // Xoa canvas trong Redis va broadcast cho tat ca
            canvasStateService.clearStrokes(roomCode);
            broadcast(roomCode, session, payload, false); // broadcast ca sender
            return;
        }

        if (RELAY_ONLY_TYPES.contains(type)) {
            // CHO PHÉP CHỈ ĐƯA TIN NHẮN ĐẾN KHÁCH HÀNG KHÁC
            broadcast(roomCode, session, payload, true); // chi broadcast cho nguoi khac
            return;
        }

        // STROKE default: luu vao Redis va broadcast
        StrokeMessage stroke = objectMapper.readValue(payload, StrokeMessage.class);
        stroke.setType(type);
        stroke.setPreview(false);
        canvasStateService.addStroke(roomCode, stroke);
        broadcast(roomCode, session, objectMapper.writeValueAsString(stroke), true); // chi broadcast cho nguoi khac
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String roomCode = sessionRoom.remove(session.getId());
        if (roomCode != null) {
            Set<WebSocketSession> roomSessions = rooms.get(roomCode);
            if (roomSessions != null) {
                roomSessions.remove(session);
                if (roomSessions.isEmpty()) rooms.remove(roomCode);
            }
        }
        System.out.println("[CoDraw] Session " + session.getId() + " disconnected");
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        afterConnectionClosed(session, CloseStatus.SERVER_ERROR);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void broadcast(String roomCode, WebSocketSession sender,
                           String payload, boolean excludeSender) {
        Set<WebSocketSession> roomSessions = rooms.get(roomCode);
        if (roomSessions == null) return;
        for (WebSocketSession s : roomSessions) {
            if (excludeSender && s.getId().equals(sender.getId())) continue;
            if (s.isOpen()) {
                try { s.sendMessage(new TextMessage(payload)); }
                catch (Exception ignored) {}
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
