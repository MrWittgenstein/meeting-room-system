package com.example.usermgmt.common.accesslog;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "access-log")
public class AccessLogProperties {

    private boolean enabled = true;
    private String loggerName = "ACCESS_LOG";
    private List<String> excludedPathPrefixes = List.of("/actuator", "/error");
}
