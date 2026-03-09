package com.codraw.CoDraw.service;

import com.codraw.CoDraw.model.StrokeMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Luu tru trang thai Canvas hien tai vao Redis.
 * Key: "room:{roomCode}:strokes"  -> List<StrokeMessage> (TTL 2 gio)
 */
@Service
public class CanvasStateService {

    private static final String KEY_PREFIX = "room:";
    private static final String KEY_SUFFIX = ":strokes";
    private static final long TTL_HOURS = 2;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public CanvasStateService(RedisTemplate<String, Object> redisTemplate,
                               ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    private String key(String roomCode) {
        return KEY_PREFIX + roomCode + KEY_SUFFIX;
    }

    /** Them mot stroke moi vao danh sach cua phong */
    public void addStroke(String roomCode, StrokeMessage stroke) {
        String key = key(roomCode);
        String json = toJson(stroke);
        if (json != null) {
            redisTemplate.opsForList().rightPush(key, json);
            redisTemplate.expire(key, TTL_HOURS, TimeUnit.HOURS);
        }
    }

    /** Lay toan bo strokes hien tai cua phong (de gui cho nguoi moi vao) */
    public List<StrokeMessage> getStrokes(String roomCode) {
        String key = key(roomCode);
        List<Object> raw = redisTemplate.opsForList().range(key, 0, -1);
        List<StrokeMessage> result = new ArrayList<>();
        if (raw != null) {
            for (Object o : raw) {
                StrokeMessage msg = fromJson(o.toString());
                if (msg != null) result.add(msg);
            }
        }
        return result;
    }

    /** Xoa canvas khi nguoi choi bam Clear */
    public void clearStrokes(String roomCode) {
        redisTemplate.delete(key(roomCode));
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return null; }
    }

    private StrokeMessage fromJson(String json) {
        try { return objectMapper.readValue(json, StrokeMessage.class); }
        catch (Exception e) { return null; }
    }
}

