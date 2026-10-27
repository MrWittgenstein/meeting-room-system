package com.uestcfir.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "cache.invalidation")
public class CacheInvalidationProperties {
    private boolean enabled = true;
    private String exchange = "cache.invalidation";
    private String queue = "";
}
