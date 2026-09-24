package com.uestcfir.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uestcfir.config.CacheConfiguration;
import com.uestcfir.pojo.entity.IotRoomStatus;
import com.uestcfir.pojo.entity.IotSensorRecord;
import com.uestcfir.pojo.entity.Meetingroom;
import com.uestcfir.service.BusinessCacheInvalidator;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.KeyGenerator;
import java.time.Duration;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BusinessCacheTest {
    @Test
    void cacheManagerOnlyCreatesAllowlistedCachesAndDisablesNullCaching() throws Exception {
        var factory = mock(RedisConnectionFactory.class);
        when(factory.getConnection()).thenReturn(mock(RedisConnection.class));
        var configuration = new CacheConfiguration();
        CacheManager manager = configuration.cacheManager(factory, new ObjectMapper(), true,
                Duration.ofMinutes(5), Duration.ofSeconds(5));
        ((org.springframework.beans.factory.InitializingBean) manager).afterPropertiesSet();
        assertNotNull(manager.getCache("meetingroom:list"));
        assertNotNull(manager.getCache("iot:latest-room"));
        assertNotNull(manager.getCache("iot:latest-device"));
        assertNotNull(manager.getCache("iot:room-status"));
        assertNotNull(manager.getCache("iot:recent-room"));
        assertNull(manager.getCache("unconfigured"));
        var redisManager = (org.springframework.data.redis.cache.RedisCacheManager) manager;
        assertFalse(redisManager.getCacheConfigurations().get("meetingroom:list").getAllowCacheNullValues());
        assertEquals(Duration.ofMinutes(5), redisManager.getCacheConfigurations().get("meetingroom:list").getTtl());
        assertEquals(Duration.ofSeconds(5), redisManager.getCacheConfigurations().get("iot:latest-room").getTtl());
        assertInstanceOf(org.springframework.cache.support.NoOpCacheManager.class,
                configuration.cacheManager(factory, new ObjectMapper(), false, Duration.ofMinutes(5), Duration.ofSeconds(5)));
        assertThrows(IllegalArgumentException.class, () -> configuration.cacheManager(factory, new ObjectMapper(), true,
                Duration.ZERO, Duration.ofSeconds(5)));
    }

    @Test
    void typedJsonSerializationRoundTripsSupportedEntities() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        var room = new Meetingroom();
        room.setRoomId(14);
        room.setOpenTime(java.time.LocalTime.of(9, 0));
        assertEquals(14, mapper.readValue(mapper.writeValueAsBytes(room), Meetingroom.class).getRoomId());
        var record = new IotSensorRecord();
        record.setRoomId(14);
        record.setTemperature(java.math.BigDecimal.valueOf(24));
        assertEquals(java.math.BigDecimal.valueOf(24),
                mapper.readValue(mapper.writeValueAsBytes(record), IotSensorRecord.class).getTemperature());
        var status = new IotRoomStatus();
        status.setRoomId(14);
        assertEquals(14, mapper.readValue(mapper.writeValueAsBytes(status), IotRoomStatus.class).getRoomId());
    }

    @Test
    void invalidationOccursOnlyAfterCommitAndCoversRecentTelemetry() {
        var manager = mock(CacheManager.class);
        Cache rooms = mock(Cache.class);
        Cache latestRoom = mock(Cache.class);
        Cache roomStatus = mock(Cache.class);
        Cache device = mock(Cache.class);
        Cache recent = mock(Cache.class);
        when(manager.getCache("meetingroom:list")).thenReturn(rooms);
        when(manager.getCache("iot:latest-room")).thenReturn(latestRoom);
        when(manager.getCache("iot:room-status")).thenReturn(roomStatus);
        when(manager.getCache("iot:latest-device")).thenReturn(device);
        when(manager.getCache("iot:recent-room")).thenReturn(recent);
        var invalidator = new BusinessCacheInvalidator(manager);
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            invalidator.roomsChanged();
            invalidator.telemetryChanged(14, "raspi-01");
            verifyNoInteractions(rooms, latestRoom, roomStatus, device, recent);
            org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations()
                    .forEach(org.springframework.transaction.support.TransactionSynchronization::afterCommit);
            verify(rooms).clear();
            verify(latestRoom).evict(14);
            verify(roomStatus).evict(14);
            verify(device).evict("raspi-01");
            verify(recent).clear();
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
            org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void invalidationIsBestEffortIfRedisCacheFails() {
        var manager = mock(CacheManager.class);
        Cache rooms = mock(Cache.class);
        when(manager.getCache("meetingroom:list")).thenReturn(rooms);
        doThrow(new IllegalStateException("Redis unavailable")).when(rooms).clear();
        assertDoesNotThrow(() -> new BusinessCacheInvalidator(manager).roomsChanged());
    }

    @Test
    void redisCacheRoundTripsTypedValuesAndUsesConfiguredExpiry() {
        var factory = new org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory("127.0.0.1", 6379);
        factory.afterPropertiesSet();
        try {
            var configuration = new CacheConfiguration();
            var manager = (org.springframework.data.redis.cache.RedisCacheManager) configuration.cacheManager(
                    factory, new ObjectMapper().findAndRegisterModules(), true, Duration.ofMinutes(5), Duration.ofSeconds(5));
            manager.afterPropertiesSet();
            Cache cache = manager.getCache("iot:latest-room");
            Integer roomId = UUID.randomUUID().hashCode();
            var record = new IotSensorRecord();
            record.setRoomId(roomId);
            record.setTemperature(java.math.BigDecimal.valueOf(24));
            String key = "cache-test-" + UUID.randomUUID();
            cache.put(key, record);
            var cached = cache.get(key, IotSensorRecord.class);
            assertNotNull(cached);
            assertEquals(roomId, cached.getRoomId());
            assertEquals(java.math.BigDecimal.valueOf(24), cached.getTemperature());
            byte[] redisKey = ("zongshe:cache:v1:iot:latest-room::" + key).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            Long ttl = factory.getConnection().keyCommands().ttl(redisKey);
            assertNotNull(ttl);
            assertTrue(ttl > 0 && ttl <= 5);
            cache.evict(key);
            assertNull(cache.get(key));
        } finally {
            factory.destroy();
        }
    }

    @Test
    void redisGetAndPutFailuresFallThroughToTargetMethod() {
        try (var context = new AnnotationConfigApplicationContext(CacheFailureContext.class)) {
            var service = context.getBean(CacheFailureService.class);
            assertEquals("database", service.lookup("room-14"));
            assertEquals("database", service.lookup("room-14"));
            assertEquals(2, context.getBean(java.util.concurrent.atomic.AtomicInteger.class).get());
        }
    }

    @Configuration
    @EnableCaching
    static class CacheFailureContext implements CachingConfigurer {
        @Bean public CacheManager cacheManager() {
            Cache cache = mock(Cache.class);
            when(cache.get(any())).thenThrow(new IllegalStateException("Redis GET failed"));
            doThrow(new IllegalStateException("Redis SET failed")).when(cache).put(any(), any());
            var manager = mock(CacheManager.class);
            when(manager.getCache("meetingroom:list")).thenReturn(cache);
            return manager;
        }
        @Bean java.util.concurrent.atomic.AtomicInteger databaseCalls() { return new java.util.concurrent.atomic.AtomicInteger(); }
        @Bean CacheFailureService cacheFailureService(java.util.concurrent.atomic.AtomicInteger calls) { return new CacheFailureService(calls); }
        @Override public CacheErrorHandler errorHandler() { return new CacheConfiguration().errorHandler(); }
    }

    static class CacheFailureService {
        private final java.util.concurrent.atomic.AtomicInteger databaseCalls;
        CacheFailureService(java.util.concurrent.atomic.AtomicInteger databaseCalls) { this.databaseCalls = databaseCalls; }
        @Cacheable(cacheNames = "meetingroom:list", key = "#p0")
        public String lookup(String room) { databaseCalls.incrementAndGet(); return "database"; }
    }
}
