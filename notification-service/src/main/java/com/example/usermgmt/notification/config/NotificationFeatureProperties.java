package com.example.usermgmt.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "notification.features")
public class NotificationFeatureProperties {

    private boolean smsEnabled = true;
    private boolean emailEnabled = true;
    private boolean siteMessageEnabled = true;
}
