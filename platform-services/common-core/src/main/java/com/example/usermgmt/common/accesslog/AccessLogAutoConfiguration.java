package com.example.usermgmt.common.accesslog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

@AutoConfiguration
@ConditionalOnClass(OncePerRequestFilter.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(AccessLogProperties.class)
public class AccessLogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "access-log", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AccessLogFilter accessLogFilter(
        ObjectMapper objectMapper,
        AccessLogProperties properties,
        Environment environment
    ) {
        return new AccessLogFilter(objectMapper, properties, environment);
    }

    @Bean
    @ConditionalOnProperty(prefix = "access-log", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<AccessLogFilter> accessLogFilterRegistration(AccessLogFilter accessLogFilter) {
        FilterRegistrationBean<AccessLogFilter> registration = new FilterRegistrationBean<>(accessLogFilter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
