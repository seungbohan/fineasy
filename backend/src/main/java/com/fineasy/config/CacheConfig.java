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
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = false)
public class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    private static final Duration BOK_TERM_CACHE_TTL = Duration.ofHours(24);
    private static final Duration AI_CACHE_TTL = Duration.ofHours(6);
    private static final Duration MACRO_CACHE_TTL = Duration.ofHours(1);
    private static final Duration POPULAR_STOCKS_CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration SHORT_CACHE_TTL = Duration.ofMinutes(10);
    private static final Duration ALERT_CACHE_TTL = Duration.ofMinutes(5);

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .entryTtl(AI_CACHE_TTL)
                .disableCachingNullValues();

        RedisCacheConfiguration bokTermConfig = defaultConfig.entryTtl(BOK_TERM_CACHE_TTL);
        RedisCacheConfiguration macroConfig = defaultConfig.entryTtl(MACRO_CACHE_TTL);
        RedisCacheConfiguration popularStocksConfig = defaultConfig.entryTtl(POPULAR_STOCKS_CACHE_TTL);
        RedisCacheConfiguration shortConfig = defaultConfig.entryTtl(SHORT_CACHE_TTL);
        RedisCacheConfiguration alertConfig = defaultConfig.entryTtl(ALERT_CACHE_TTL);

        Map<String, RedisCacheConfiguration> cacheConfigs = Map.ofEntries(
                Map.entry("bok-term-explanation", bokTermConfig),
                Map.entry("analysis-report", defaultConfig),
                Map.entry("analysis-prediction", defaultConfig),
                Map.entry("macro-indicators", macroConfig),
                Map.entry("popular-stocks", popularStocksConfig),
                Map.entry("crypto-prices", popularStocksConfig),
                Map.entry("market-risk-summary", popularStocksConfig),
                Map.entry("macro-indicators-category", shortConfig),
                Map.entry("global-events", shortConfig),
                Map.entry("global-events-alerts", alertConfig)
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
