package com.uestcfir.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.Duration;

@Configuration
public class PlatformServiceClientConfig {
    @Bean
    @LoadBalanced
    @ConditionalOnProperty(prefix = "platform-services", name = "discovery-enabled", havingValue = "true")
    public RestTemplate platformRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Bean(name = "platformRestTemplate")
    @ConditionalOnProperty(prefix = "platform-services", name = "discovery-enabled", havingValue = "false", matchIfMissing = true)
    public RestTemplate directPlatformRestTemplate(RestTemplateBuilder builder) {
        return builder.setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(5)).build();
    }
}
