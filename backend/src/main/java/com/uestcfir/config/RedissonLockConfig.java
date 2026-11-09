package com.uestcfir.config;


import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.ObjectInputFilter;

@Configuration
@Data
public class RedissonLockConfig {



    @Bean
    public RedissonClient redissonClient(
            @Value("${reservation.redis-address:redis://127.0.0.1:6379}") String redisAddress) {
        Config config = new Config();
        config.useSingleServer()
                .setAddress(redisAddress)
                .setDatabase(0)
                .setConnectionMinimumIdleSize(5)
                .setConnectionPoolSize(10)
                .setSubscriptionConnectionPoolSize(5)
                .setConnectTimeout(10000)
                .setTimeout(3000)
                .setIdleConnectionTimeout(30000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);
        config.setThreads(16)
                .setNettyThreads(32);
        return Redisson.create(config);
    }
}

