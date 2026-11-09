package com.uestcfir.cacheinvalidation;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;

class CacheBridgeContextTest {
    @Test void nonWebApplicationCreatesRunnerAndSerializesEventTime() {
        new ApplicationContextRunner().withUserConfiguration(CacheInvalidationApplication.class)
                .withPropertyValues("spring.rabbitmq.listener.simple.auto-startup=false")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(CanalCacheInvalidationRunner.class);
                    String json = context.getBean(ObjectMapper.class).writeValueAsString(
                            java.util.Map.of("occurredAt", Instant.parse("2026-11-09T00:00:00Z")));
                    assertThat(json).contains("occurredAt");
                });
    }
}
