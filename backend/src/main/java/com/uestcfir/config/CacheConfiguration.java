package com.uestcfir.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uestcfir.pojo.entity.IotRoomStatus;
import com.uestcfir.pojo.entity.IotSensorRecord;
import com.uestcfir.pojo.entity.Meetingroom;
import com.uestcfir.pojo.vo.IotTelemetryVo;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.BatchStrategies;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfiguration implements CachingConfigurer {
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory, ObjectMapper mapper,
            @Value("${business-cache.enabled:true}") boolean enabled,
            @Value("${business-cache.room-list-ttl:5m}") Duration roomTtl,
            @Value("${business-cache.telemetry-ttl:5s}") Duration telemetryTtl) {
        if (!enabled) return new NoOpCacheManager();
        if (roomTtl.isZero() || roomTtl.isNegative() || telemetryTtl.isZero() || telemetryTtl.isNegative()) {
            throw new IllegalArgumentException("Business cache TTLs must be positive");
        }
        var recordConfig = configuration(mapper, mapper.constructType(IotSensorRecord.class), telemetryTtl);
        return RedisCacheManager.builder(RedisCacheWriter.nonLockingRedisCacheWriter(factory, BatchStrategies.scan(500)))
                .withInitialCacheConfigurations(Map.of(
                        "meetingroom:list", configuration(mapper, mapper.getTypeFactory().constructCollectionType(List.class, Meetingroom.class), roomTtl),
                        "iot:latest-room", recordConfig,
                        "iot:latest-device", recordConfig,
                        "iot:recent-room", configuration(mapper,
                                mapper.getTypeFactory().constructCollectionType(List.class, IotTelemetryVo.class), telemetryTtl),
                        "iot:room-status", configuration(mapper, mapper.constructType(IotRoomStatus.class), telemetryTtl)))
                .disableCreateOnMissingCache().build();
    }

    private RedisCacheConfiguration configuration(ObjectMapper mapper, JavaType type, Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig().entryTtl(ttl).disableCachingNullValues()
                .computePrefixWith(name -> "zongshe:cache:v1:" + name + "::")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new Jackson2JsonRedisSerializer<>(mapper.copy(), type)));
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            private void report(String operation, Cache cache, RuntimeException exception) {
                LoggerFactory.getLogger(CacheConfiguration.class).warn("Business cache {} failed for {} ({})",
                        operation, cache.getName(), exception.getClass().getSimpleName());
            }
            public void handleCacheGetError(RuntimeException e, Cache c, Object key) { report("read", c, e); }
            public void handleCachePutError(RuntimeException e, Cache c, Object key, Object value) { report("write", c, e); }
            public void handleCacheEvictError(RuntimeException e, Cache c, Object key) { report("evict", c, e); }
            public void handleCacheClearError(RuntimeException e, Cache c) { report("clear", c, e); }
        };
    }
}
