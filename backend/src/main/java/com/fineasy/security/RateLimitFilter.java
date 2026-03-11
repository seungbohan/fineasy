package com.fineasy.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fineasy.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_MS = 60_000L;

    private final ObjectMapper objectMapper;
    private final Map<String, RateInfo> rateLimitMap = new ConcurrentHashMap<>();

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (!path.startsWith("/api/v1/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        String key = clientIp + ":" + path;

        RateInfo rateInfo = rateLimitMap.compute(key, (k, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null || now - existing.windowStart > WINDOW_MS) {
                return new RateInfo(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (rateInfo.count.get() > MAX_REQUESTS) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            ApiResponse<Void> body = ApiResponse.error(
                    "TOO_MANY_REQUESTS", "Too many requests. Please try again later.");
            objectMapper.writeValue(response.getOutputStream(), body);
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

    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void evictExpiredEntries() {
        long now = System.currentTimeMillis();
        int before = rateLimitMap.size();
        rateLimitMap.entrySet().removeIf(entry -> now - entry.getValue().windowStart > WINDOW_MS);
        int removed = before - rateLimitMap.size();
        if (removed > 0) {

            org.slf4j.LoggerFactory.getLogger(RateLimitFilter.class)
                    .debug("Evicted {} expired rate limit entries", removed);
        }
    }

    private static class RateInfo {
        final long windowStart;
        final AtomicInteger count;

        RateInfo(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
