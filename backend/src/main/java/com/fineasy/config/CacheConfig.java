package com.fineasy.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = false)
public class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    private static final Duration STATIC_CACHE_TTL = Duration.ofHours(24);
    private static final Duration AI_CACHE_TTL = Duration.ofHours(6);
    private static final Duration LONG_CACHE_TTL = Duration.ofHours(1);
    private static final Duration MEDIUM_CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration SHORT_CACHE_TTL = Duration.ofMinutes(10);
    private static final Duration VERY_SHORT_CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration REALTIME_CACHE_TTL = Duration.ofMinutes(1);

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .entryTtl(AI_CACHE_TTL)
                .disableCachingNullValues();

        RedisCacheConfiguration staticConfig = defaultConfig.entryTtl(STATIC_CACHE_TTL);
        RedisCacheConfiguration longConfig = defaultConfig.entryTtl(LONG_CACHE_TTL);
        RedisCacheConfiguration mediumConfig = defaultConfig.entryTtl(MEDIUM_CACHE_TTL);
        RedisCacheConfiguration shortConfig = defaultConfig.entryTtl(SHORT_CACHE_TTL);
        RedisCacheConfiguration veryShortConfig = defaultConfig.entryTtl(VERY_SHORT_CACHE_TTL);
        RedisCacheConfiguration realtimeConfig = defaultConfig.entryTtl(REALTIME_CACHE_TTL);

        Map<String, RedisCacheConfiguration> cacheConfigs = Map.ofEntries(
                // AI generated content (6h)
                Map.entry("analysis-report", defaultConfig),
                Map.entry("analysis-prediction", defaultConfig),
                // Static data (24h)
                Map.entry("etf-presets", staticConfig),
                Map.entry("sector-contents-list", staticConfig),
                Map.entry("sector-contents-detail", staticConfig),
                Map.entry("bok-term-explanation", staticConfig),
                Map.entry("terms-all", staticConfig),
                Map.entry("terms-categories", staticConfig),
                Map.entry("terms-by-category", staticConfig),
                Map.entry("terms-detail", staticConfig),
                // Long cache (1h)
                Map.entry("macro-indicators", longConfig),
                Map.entry("stock-financials", longConfig),
                Map.entry("stock-fundamentals", longConfig),
                Map.entry("sector-comparison", longConfig),
                Map.entry("market-summary", longConfig),
                // Medium cache (30m)
                Map.entry("popular-stocks", mediumConfig),
                Map.entry("crypto-prices", mediumConfig),
                Map.entry("market-risk-summary", mediumConfig),
                // Short cache (10m)
                Map.entry("macro-indicators-category", shortConfig),
                Map.entry("global-events", shortConfig),
                Map.entry("stock-info", shortConfig),
                Map.entry("stock-chart", shortConfig),
                Map.entry("market-ranking", shortConfig),
                // Very short cache (5m)
                Map.entry("global-events-alerts", veryShortConfig),
                Map.entry("news-list", veryShortConfig),
                // Realtime cache (1m)
                Map.entry("stock-price", realtimeConfig)
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("Redis cache GET error [cache={}, key={}]: {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("Redis cache PUT error [cache={}, key={}]: {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                log.warn("Redis cache EVICT error [cache={}, key={}]: {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.warn("Redis cache CLEAR error [cache={}]: {}", cache.getName(), e.getMessage());
            }
        };
    }
}
