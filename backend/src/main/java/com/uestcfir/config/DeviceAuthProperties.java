package com.uestcfir.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.HashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Data
@ConfigurationProperties(prefix = "iot.device-auth")
public class DeviceAuthProperties {
    private Map<String, String> tokens = new HashMap<>();

    public boolean valid(String deviceId, String token) {
        if (deviceId == null || deviceId.isBlank() || token == null || token.isBlank()) return false;
        String expected = tokens.get(deviceId);
        return expected != null && expected.length() >= 32 && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
    }
}
