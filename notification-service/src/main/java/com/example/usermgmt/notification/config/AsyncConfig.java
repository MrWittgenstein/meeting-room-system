package com.example.usermgmt.notification.config;

import com.example.usermgmt.common.accesslog.TraceContext;
import java.util.concurrent.Executor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

    @Bean("captchaSendExecutor")
    public Executor captchaSendExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("captcha-send-");
        executor.setTaskDecorator(task -> {
            String traceId = TraceContext.getTraceId();
            return () -> {
                try {
                    TraceContext.setTraceId(traceId);
                    if (traceId != null && !traceId.isBlank()) {
                        MDC.put("traceId", traceId);
                    }
                    task.run();
                } finally {
                    MDC.remove("traceId");
                    TraceContext.clear();
                }
            };
        });
        executor.initialize();
        return executor;
    }
}
