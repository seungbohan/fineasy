package com.fineasy.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed token blacklist for logout/revocation.
 * Stores blacklisted JWT token IDs with TTL matching token expiration.
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);
    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    public void blacklist(String token, long ttlSeconds) {
        if (redisTemplate == null) {
            log.debug("Redis unavailable, token blacklist not stored");
            return;
        }
        try {
            String key = BLACKLIST_PREFIX + hashToken(token);
            redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.warn("Failed to blacklist token: {}", e.getMessage());
        }
    }

    public boolean isBlacklisted(String token) {
        if (redisTemplate == null) return false;
        try {
            String key = BLACKLIST_PREFIX + hashToken(token);
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("Failed to check token blacklist: {}", e.getMessage());
            return false;
        }
    }

    private String hashToken(String token) {
        // Use last 32 chars of token as key (unique enough, saves memory)
        return token.length() > 32 ? token.substring(token.length() - 32) : token;
    }
}
