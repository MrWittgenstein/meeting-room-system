package com.uestcfir.cache;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.AsyncConsumerStartedEvent;
import org.springframework.test.util.ReflectionTestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CacheSubscriberRecoveryTest {
    @Test void onlyCacheSubscriberRecoveryClearsWarmEntries() {
        var manager = new ConcurrentMapCacheManager("meetingroom:list");
        var cache = manager.getCache("meetingroom:list");
        cache.put("1:10", "stale");
        var consumer = new CacheInvalidationConsumer(new ObjectMapper(), manager);
        ReflectionTestUtils.setField(consumer, "queue", new Queue("cache-instance-1"));
        var unrelated = mock(SimpleMessageListenerContainer.class);
        when(unrelated.getQueueNames()).thenReturn(new String[]{"business-events"});
        consumer.subscriberStarted(new AsyncConsumerStartedEvent(unrelated, new Object()));
        assertNotNull(cache.get("1:10"));
        var subscriber = mock(SimpleMessageListenerContainer.class);
        when(subscriber.getQueueNames()).thenReturn(new String[]{"cache-instance-1"});
        consumer.subscriberStarted(new AsyncConsumerStartedEvent(subscriber, new Object()));
        assertNull(cache.get("1:10"));
    }
}
