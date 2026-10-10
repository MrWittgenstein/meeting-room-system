package com.example.usermgmt.common.accesslog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class AccessLogFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_ATTRIBUTE = AccessLogFilter.class.getName() + ".traceId";
    public static final String USER_ID_ATTRIBUTE = AccessLogFilter.class.getName() + ".userId";

    private final ObjectMapper objectMapper;
    private final AccessLogProperties properties;
    private final String applicationName;
    private final Logger accessLogger;

    public AccessLogFilter(ObjectMapper objectMapper, AccessLogProperties properties, Environment environment) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.applicationName = Optional.ofNullable(environment.getProperty("spring.application.name")).orElse("unknown-service");
        this.accessLogger = LoggerFactory.getLogger(properties.getLoggerName());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        long startTime = System.nanoTime();
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
        Exception failure = null;
        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException exception) {
            failure = exception;
            throw exception;
        } finally {
            try {
                if (!shouldSkip(request)) {
                    writeAccessLog(request, response, startTime, failure);
                }
            } finally {
                MDC.remove("traceId");
                TraceContext.clear();
            }
        }
    }

    private void writeAccessLog(HttpServletRequest request, HttpServletResponse response, long startTime, Exception failure) {
        long latencyMs = Math.max(0L, (System.nanoTime() - startTime) / 1_000_000L);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("timestamp", Instant.now().toString());
        payload.put("user_agent", safeValue(request.getHeader("User-Agent")));
        payload.put("client_ip", resolveClientIp(request));
        payload.put("trace_id", String.valueOf(request.getAttribute(TRACE_ID_ATTRIBUTE)));
        payload.put("user_id", safeValue(firstNonBlank(
            request.getHeader("X-User-Id"),
            request.getHeader("User-Id"),
            attributeValue(request, USER_ID_ATTRIBUTE)
        )));
        payload.put("microservice", applicationName);
        payload.put("api_endpoint", request.getRequestURI());
        payload.put("http_method", request.getMethod());
        payload.put("status_code", response.getStatus());
        payload.put("latency_ms", latencyMs);
        payload.put("query_string", safeValue(request.getQueryString()));
        payload.put("exception", failure == null ? "" : failure.getClass().getSimpleName());
        try {
            accessLogger.info(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException exception) {
            accessLogger.warn("{\"timestamp\":\"{}\",\"microservice\":\"{}\",\"api_endpoint\":\"{}\",\"status_code\":{},\"latency_ms\":{}}",
                Instant.now(), applicationName, request.getRequestURI(), response.getStatus(), latencyMs);
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

    private String attributeValue(HttpServletRequest request, String name) {
        Object value = request.getAttribute(name);
        return value == null ? "" : String.valueOf(value);
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
