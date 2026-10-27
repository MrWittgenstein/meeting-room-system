package com.uestcfir.cacheinvalidation;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "cache-invalidation")
public class CacheInvalidationProperties {
    private String canalHost = "127.0.0.1";
    private int canalPort = 11111;
    private String canalDestination = "meetingroom";
    private String canalUsername = "canal";
    private String canalPassword = "canal";
    private String rabbitExchange = "cache.invalidation";
    private String rabbitRoutingKey = "cache.invalidate";
    private int batchSize = 100;
}
