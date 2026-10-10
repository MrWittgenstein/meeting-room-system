package com.example.usermgmt.userservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "user-service.features")
public class UserServiceFeatureProperties {

    private boolean passwordLoginEnabled = true;
    private boolean codeLoginEnabled = true;
    private boolean oauthLoginEnabled = true;
    private boolean registerEnabled = true;
    private int passwordMaxAttempts = 5;
    private int passwordLockMinutes = 15;
}
