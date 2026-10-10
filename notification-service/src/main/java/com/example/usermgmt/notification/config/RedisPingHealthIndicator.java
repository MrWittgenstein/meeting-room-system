package com.example.usermgmt.notification.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component("redisHealthIndicator")
@RequiredArgsConstructor
public class RedisPingHealthIndicator implements HealthIndicator {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Health health() {
        try {
            String pong = stringRedisTemplate.execute((RedisCallback<String>) connection -> connection.ping());
            if ("PONG".equalsIgnoreCase(pong)) {
                return Health.up().withDetail("ping", pong).build();
            }
            return Health.unknown().withDetail("ping", pong).build();
        } catch (Exception exception) {
            return Health.down(exception).build();
        }
    }
}
