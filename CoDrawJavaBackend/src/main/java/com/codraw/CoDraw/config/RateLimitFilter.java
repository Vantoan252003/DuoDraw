package com.codraw.CoDraw.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory sliding-window rate limiter.
 * Limits each client IP to a configurable number of requests per minute.
 * Returns HTTP 429 Too Many Requests when the limit is exceeded.
 */
@Component
@Order(1) // Run before JwtAuthFilter
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final int MAX_REQUESTS_PER_MINUTE = 120;
    private static final long WINDOW_MS = 60_000L;

    /** Tracks request count and window start per client IP. */
    private final Map<String, RateBucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Skip WebSocket upgrade requests — they are rate-limited at the message level
        if ("websocket".equalsIgnoreCase(request.getHeader("Upgrade"))) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        RateBucket bucket = buckets.computeIfAbsent(clientIp, k -> new RateBucket());

        if (!bucket.tryConsume()) {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Try again later.\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /** Simple sliding-window counter bucket. */
    private static class RateBucket {
        private long windowStart = System.currentTimeMillis();
        private final AtomicInteger count = new AtomicInteger(0);

        synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();
            if (now - windowStart > WINDOW_MS) {
                // Reset window
                windowStart = now;
                count.set(0);
            }
            return count.incrementAndGet() <= MAX_REQUESTS_PER_MINUTE;
        }
    }
}
