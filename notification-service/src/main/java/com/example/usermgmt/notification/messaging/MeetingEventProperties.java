package com.example.usermgmt.notification.messaging;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "meeting.events")
public class MeetingEventProperties {
    private boolean enabled = true;
    private String exchange = "meeting.events";
    private String queue = "meeting.notification.events";
    private String deadLetterExchange = "meeting.events.dlx";
    private String deadLetterQueue = "meeting.notification.events.dlq";
    private String deadLetterRoutingKey = "notification.failed";
    private int listenerPrefetch = 20;
    private int listenerMaxAttempts = 3;
}
