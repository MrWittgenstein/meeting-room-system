package com.uestcfir.service;

import com.uestcfir.config.PlatformServiceClientConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import static org.assertj.core.api.Assertions.assertThat;

class PlatformServiceClientConfigTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(RestTemplateBuilder.class, RestTemplateBuilder::new)
            .withUserConfiguration(PlatformServiceClientConfig.class);
    @Test void fixedUrlsDoNotUseServiceDiscovery() {
        runner.withPropertyValues("platform-services.discovery-enabled=false").run(context -> {
            assertThat(context).hasNotFailed().hasBean("platformRestTemplate");
            assertThat(context.getBeanFactory().findAnnotationOnBean("platformRestTemplate", LoadBalanced.class)).isNull();
        });
    }
    @Test void discoveryClientRetainsLoadBalancing() {
        runner.withPropertyValues("platform-services.discovery-enabled=true").run(context -> {
            assertThat(context).hasNotFailed().hasBean("platformRestTemplate");
            assertThat(context.getBeanFactory().findAnnotationOnBean("platformRestTemplate", LoadBalanced.class)).isNotNull();
        });
    }
}
