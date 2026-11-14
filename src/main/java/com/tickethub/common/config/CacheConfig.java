package com.tickethub.common.config;

import com.tickethub.common.dto.PageResponse;
import com.tickethub.event.dto.EventCardResponse;
import com.tickethub.event.dto.PublicEventResponse;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.BatchStrategies;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    public static final String EVENT_SEARCH = "eventSearch";
    public static final String EVENT_DETAILS = "eventDetails";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, JsonMapper jsonMapper) {
        // Keys look like "tickethub:eventDetails::5"; null results are never cached
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith("tickethub:")
                .disableCachingNullValues();

        // One typed JSON serializer per cache: readable values in Redis, no class names stored
        JavaType searchType = jsonMapper.getTypeFactory()
                .constructParametricType(PageResponse.class, EventCardResponse.class);

        return RedisCacheManager.builder(
                        // SCAN instead of KEYS when clearing a whole cache (KEYS blocks Redis on big databases)
                        RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory, BatchStrategies.scan(1000)))
                .withCacheConfiguration(EVENT_SEARCH, defaults
                        .entryTtl(Duration.ofSeconds(60)) // results depend on "now", so keep them short-lived
                        .serializeValuesWith(SerializationPair.fromSerializer(
                                new JacksonJsonRedisSerializer<>(jsonMapper, searchType))))
                .withCacheConfiguration(EVENT_DETAILS, defaults
                        .entryTtl(Duration.ofMinutes(5))
                        .serializeValuesWith(SerializationPair.fromSerializer(
                                new JacksonJsonRedisSerializer<>(jsonMapper, PublicEventResponse.class))))
                .disableCreateOnMissingCache() // a typo in a cache name fails loudly instead of creating a new cache
                .transactionAware()            // evictions happen after the database commit, not before
                .build();
    }

    // If Redis is down: log it and run the method normally (read from Postgres) instead of failing the request
    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }
}