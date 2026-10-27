package com.uestcfir.cache;

import java.util.UUID;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
@ConditionalOnProperty(prefix = "cache.invalidation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CacheInvalidationRabbitConfiguration {

    @Bean
    public TopicExchange cacheInvalidationExchange(CacheInvalidationProperties properties) {
        return new TopicExchange(properties.getExchange(), true, false);
    }

    @Bean(name = "cacheInvalidationQueue")
    public Queue cacheInvalidationQueue(CacheInvalidationProperties properties,
                                        org.springframework.core.env.Environment environment) {
        String configured = properties.getQueue();
        String application = environment.getProperty("spring.application.name", "meetingroom-main");
        String queueName = configured == null || configured.isBlank()
                ? properties.getExchange() + "." + application + "." + UUID.randomUUID()
                : configured;
        return QueueBuilder.nonDurable(queueName).exclusive().autoDelete().build();
    }

    @Bean
    public Binding cacheInvalidationBinding(
            @Qualifier("cacheInvalidationQueue") Queue cacheInvalidationQueue,
            TopicExchange cacheInvalidationExchange
    ) {
        return BindingBuilder.bind(cacheInvalidationQueue).to(cacheInvalidationExchange).with("#");
    }
}
