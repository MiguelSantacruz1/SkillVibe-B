package com.skillvibe.tutoring.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Cache<String, TokenBucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .maximumSize(10000)
            .build();

    // Limit configuration
    private static final long CAPACITY = 10;
    private static final long REFILL_TOKENS = 10;
    private static final long REFILL_DURATION_MS = 60000; // 1 minute

    private static class TokenBucket {
        private long tokens;
        private long lastRefillTimestamp;

        public TokenBucket() {
            this.tokens = CAPACITY;
            this.lastRefillTimestamp = Instant.now().toEpochMilli();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (this.tokens > 0) {
                this.tokens--;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = Instant.now().toEpochMilli();
            long elapsedTime = now - lastRefillTimestamp;
            
            if (elapsedTime > REFILL_DURATION_MS) {
                long tokensToAdd = (elapsedTime / REFILL_DURATION_MS) * REFILL_TOKENS;
                this.tokens = Math.min(CAPACITY, this.tokens + tokensToAdd);
                this.lastRefillTimestamp = now;
            }
        }
    }

    private TokenBucket resolveBucket(String ip) {
        return buckets.get(ip, k -> new TokenBucket());
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }

        TokenBucket bucket = resolveBucket(clientIp);

        if (bucket.tryConsume()) {
            return true;
        } else {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Too many requests. Please try again later.");
            return false;
        }
    }
}
