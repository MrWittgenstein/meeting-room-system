package com.example.usermgmt.notification.messaging;

import java.time.Instant;
import java.util.Map;

public record MeetingEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        Map<String, Object> payload
) {
}
