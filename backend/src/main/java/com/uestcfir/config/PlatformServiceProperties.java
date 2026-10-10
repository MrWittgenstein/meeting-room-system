package com.uestcfir.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "platform-services")
public class PlatformServiceProperties {
    private boolean enabled;
    private String internalToken;
    private String userServiceUrl;
    private String notificationServiceUrl;
    private boolean discoveryEnabled;
    private String userServiceName = "meeting-user-service";
    private String notificationServiceName = "meeting-notification-service";
}
