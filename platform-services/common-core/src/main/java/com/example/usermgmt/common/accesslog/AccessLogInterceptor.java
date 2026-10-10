package com.example.usermgmt.common.accesslog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

public class AccessLogInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTRIBUTE = AccessLogInterceptor.class.getName() + ".startTime";
    private static final String TRACE_ID_ATTRIBUTE = AccessLogInterceptor.class.getName() + ".traceId";

    private final ObjectMapper objectMapper;
    private final AccessLogProperties properties;
    private final String applicationName;
    private final Logger accessLogger;

    public AccessLogInterceptor(ObjectMapper objectMapper, AccessLogProperties properties, Environment environment) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.applicationName = Optional.ofNullable(environment.getProperty("spring.application.name")).orElse("unknown-service");
        this.accessLogger = LoggerFactory.getLogger(properties.getLoggerName());
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTRIBUTE, System.nanoTime());
        String traceId = firstNonBlank(
            request.getHeader(TraceContext.TRACE_ID_HEADER),
            request.getHeader("Trace-Id"),
            request.getHeader("X-Request-Id"),
            UUID.randomUUID().toString()
        );
        request.setAttribute(TRACE_ID_ATTRIBUTE, traceId);
        TraceContext.setTraceId(traceId);
        MDC.put("traceId", traceId);
        response.setHeader(TraceContext.TRACE_ID_HEADER, traceId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (shouldSkip(request)) {
            MDC.remove("traceId");
            TraceContext.clear();
            return;
        }
        long startTime = Optional.ofNullable(request.getAttribute(START_TIME_ATTRIBUTE))
            .filter(Long.class::isInstance)
            .map(Long.class::cast)
            .orElse(System.nanoTime());
        long latencyMs = Math.max(0L, (System.nanoTime() - startTime) / 1_000_000L);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("timestamp", Instant.now().toString());
        payload.put("user_agent", safeValue(request.getHeader("User-Agent")));
        payload.put("client_ip", resolveClientIp(request));
        payload.put("trace_id", String.valueOf(request.getAttribute(TRACE_ID_ATTRIBUTE)));
        payload.put("user_id", safeValue(firstNonBlank(request.getHeader("X-User-Id"), request.getHeader("User-Id"))));
        payload.put("microservice", applicationName);
        payload.put("api_endpoint", request.getRequestURI());
        payload.put("http_method", request.getMethod());
        payload.put("status_code", response.getStatus());
        payload.put("latency_ms", latencyMs);
        payload.put("query_string", safeValue(request.getQueryString()));
        payload.put("exception", ex == null ? "" : ex.getClass().getSimpleName());
        try {
            accessLogger.info(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException exception) {
            accessLogger.warn("{\"timestamp\":\"{}\",\"microservice\":\"{}\",\"api_endpoint\":\"{}\",\"status_code\":{},\"latency_ms\":{}}",
                Instant.now(), applicationName, request.getRequestURI(), response.getStatus(), latencyMs);
        } finally {
            MDC.remove("traceId");
            TraceContext.clear();
        }
    }

    private boolean shouldSkip(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        return properties.getExcludedPathPrefixes().stream()
            .filter(StringUtils::hasText)
            .anyMatch(path::startsWith);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return firstNonBlank(
            request.getHeader("X-Real-IP"),
            request.getHeader("Proxy-Client-IP"),
            request.getHeader("WL-Proxy-Client-IP"),
            request.getRemoteAddr()
        );
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }
}
