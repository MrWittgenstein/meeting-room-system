package com.uestcfir.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "cache.invalidation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CacheInvalidationConsumer {
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;

    /** Exclusive queues disappear on broker restart. Drop entries cached while
     * disconnected whenever this subscriber starts (including recovery). */
    @org.springframework.context.event.EventListener
    public void subscriberStarted(org.springframework.amqp.rabbit.listener.AsyncConsumerStartedEvent event) {
        if (event.getSource() instanceof org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer container
                && java.util.Arrays.stream(container.getQueueNames()).anyMatch(name -> name.equals(queue.getName()))) {
            resetCaches();
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.beans.factory.annotation.Qualifier("cacheInvalidationQueue")
    private org.springframework.amqp.core.Queue queue;

    public void resetCaches() {
        for (String name : cacheManager.getCacheNames()) {
            var cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        }
        log.info("Cache invalidation subscriber started; cleared caches after connection recovery");
    }

    @RabbitListener(queues = "#{@cacheInvalidationQueue.name}")
    public void consume(Message message) throws Exception {
        CacheInvalidationEvent event = objectMapper.readValue(message.getBody(), CacheInvalidationEvent.class);
        var cache = cacheManager.getCache(event.cacheName());
        if (cache == null) {
            log.warn("Ignoring invalidation for unknown cache {}", event.cacheName());
            return;
        }
        if (event.clear() || event.key() == null || event.key().isBlank()) {
            cache.clear();
        } else {
            cache.evict(event.key());
        }
        log.debug("Cache invalidated: cache={}, key={}, source={}", event.cacheName(), event.key(), event.source());
    }
}
