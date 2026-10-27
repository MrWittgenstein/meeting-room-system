package com.uestcfir.cache;

import java.time.Instant;

public record CacheInvalidationEvent(
        String eventId,
        String cacheName,
        String key,
        boolean clear,
        String source,
        Instant occurredAt
) {
}
