package com.codraw.CoDraw.service;

import com.codraw.CoDraw.model.StrokeMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Canvas state persistence via Redis.
 * Key: "room:{roomCode}:strokes" → List<StrokeMessage> (TTL 2 hours)
 *
 * Uses Redis pipelining for batch operations to reduce network round-trips.
 */
@Service
public class CanvasStateService {

    private static final Logger log = LoggerFactory.getLogger(CanvasStateService.class);

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

    /** Add a single stroke to the room's canvas list. */
    public void addStroke(String roomCode, StrokeMessage stroke) {
        String key = key(roomCode);
        String json = toJson(stroke);
        if (json != null) {
            redisTemplate.opsForList().rightPush(key, json);
            redisTemplate.expire(key, TTL_HOURS, TimeUnit.HOURS);
        }
    }

    /** Get all strokes for a room (used when a new player joins). */
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

    /** Clear all strokes for a room. */
    public void clearStrokes(String roomCode) {
        redisTemplate.delete(key(roomCode));
    }

    /**
     * Remove the last stroke of a specific player from the room.
     * Uses Redis pipeline to perform delete + re-push in a single round-trip.
     */
    public StrokeMessage removeLastStrokeForPlayer(String roomCode, int playerId) {
        String redisKey = key(roomCode);
        List<Object> raw = redisTemplate.opsForList().range(redisKey, 0, -1);
        if (raw == null || raw.isEmpty()) {
            return null;
        }

        List<StrokeMessage> strokes = new ArrayList<>();
        for (Object item : raw) {
            StrokeMessage message = fromJson(item.toString());
            if (message != null) {
                strokes.add(message);
            }
        }

        // Find the last stroke for this player (non-preview)
        StrokeMessage removedStroke = null;
        for (int index = strokes.size() - 1; index >= 0; index--) {
            StrokeMessage stroke = strokes.get(index);
            if (stroke.getPlayerId() == playerId && !stroke.isPreview()) {
                removedStroke = stroke;
                strokes.remove(index);
                break;
            }
        }

        if (removedStroke == null) {
            return null;
        }

        // Pipeline: delete key, re-push all remaining strokes, set TTL — single round-trip
        final List<StrokeMessage> remaining = strokes;
        try {
            redisTemplate.executePipelined(new SessionCallback<Object>() {
                @Override
                @SuppressWarnings("unchecked")
                public Object execute(RedisOperations operations) throws DataAccessException {
                    operations.delete(redisKey);
                    for (StrokeMessage s : remaining) {
                        String json = toJson(s);
                        if (json != null) {
                            operations.opsForList().rightPush(redisKey, json);
                        }
                    }
                    if (!remaining.isEmpty()) {
                        operations.expire(redisKey, TTL_HOURS, TimeUnit.HOURS);
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            log.error("Redis pipeline error during undo for room {}: {}", roomCode, e.getMessage());
        }

        return removedStroke;
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) {
            log.warn("JSON serialization error: {}", e.getMessage());
            return null;
        }
    }

    private StrokeMessage fromJson(String json) {
        try { return objectMapper.readValue(json, StrokeMessage.class); }
        catch (Exception e) {
            log.warn("JSON deserialization error: {}", e.getMessage());
            return null;
        }
    }
}
