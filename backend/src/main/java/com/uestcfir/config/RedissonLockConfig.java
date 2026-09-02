package com.uestcfir.config;


import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.ObjectInputFilter;

@Configuration
@Data
public class RedissonLockConfig {



    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://127.0.0.1:6379")
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

