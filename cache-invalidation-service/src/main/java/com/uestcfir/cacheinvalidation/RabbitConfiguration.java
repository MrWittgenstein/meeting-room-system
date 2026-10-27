package com.uestcfir.cacheinvalidation;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfiguration {
    @Bean
    public TopicExchange cacheInvalidationExchange(CacheInvalidationProperties properties) {
        return new TopicExchange(properties.getRabbitExchange(), true, false);
    }
}
