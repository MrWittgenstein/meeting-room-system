package com.uestcfir.messaging;

import java.time.Instant;
import java.util.Map;

/** A versioned-by-contract event envelope shared with notification-service. */
public record MeetingEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        Map<String, Object> payload
) {
}
