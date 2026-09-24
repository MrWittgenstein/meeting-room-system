package com.uestcfir.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Emits one JSON access event per HTTP request without recording credentials or request bodies. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccessLogFilter extends OncePerRequestFilter {
    private static final Logger ACCESS_LOG = LoggerFactory.getLogger("ACCESS_LOG");
    private static final String TRACE_ATTRIBUTE = AccessLogFilter.class.getName() + ".traceId";

    private final ObjectMapper objectMapper;

    public AccessLogFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long started = System.nanoTime();
        String supplied = request.getHeader(TraceContext.HEADER);
        String traceId = supplied != null && supplied.matches("[a-zA-Z0-9-]{1,64}")
                ? supplied : UUID.randomUUID().toString();
        request.setAttribute(TRACE_ATTRIBUTE, traceId);
        TraceContext.set(traceId);
        MDC.put("trace_id", traceId);
        response.setHeader(TraceContext.HEADER, traceId);
        Exception failure = null;
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException exception) {
            failure = exception;
            throw exception;
        } finally {
            try {
                if (!request.getRequestURI().startsWith("/actuator") && !request.getRequestURI().equals("/error")) {
                    writeEvent(request, response, started, failure);
                }
            } finally {
                MDC.remove("trace_id");
                TraceContext.clear();
            }
        }
    }

    private void writeEvent(HttpServletRequest request, HttpServletResponse response, long started, Exception failure) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("event_id", UUID.randomUUID().toString());
        event.put("timestamp", Instant.now().toString());
        event.put("microservice", "meetingroom-main");
        event.put("api_endpoint", request.getRequestURI());
        event.put("http_method", request.getMethod());
        event.put("status_code", response.getStatus());
        event.put("business_code", request.getAttribute("businessCode") != null ? request.getAttribute("businessCode")
                : failure != null ? 500 : response.getStatus() >= 400 ? response.getStatus() : 1);
        event.put("latency_ms", Math.max(0L, (System.nanoTime() - started) / 1_000_000L));
        event.put("trace_id", String.valueOf(request.getAttribute(TRACE_ATTRIBUTE)));
        Object userId = request.getAttribute("UserId");
        event.put("user_id", userId == null ? "" : String.valueOf(userId));
        event.put("client_ip", resolveClientIp(request));
        event.put("user_agent", safe(request.getHeader("User-Agent")));
        // Query values and headers can contain credentials under arbitrary names.
        event.put("device_id", request.getAttribute("deviceId") == null ? "" : request.getAttribute("deviceId"));
        event.put("room_id", request.getAttribute("roomId") == null ? "" : String.valueOf(request.getAttribute("roomId")));
        event.put("command_id", request.getAttribute("commandId") == null ? "" : request.getAttribute("commandId"));
        event.put("outcome", request.getAttribute("outcome") == null ? "" : request.getAttribute("outcome"));
        event.put("exception", failure == null ? "" : failure.getClass().getSimpleName());
        try {
            ACCESS_LOG.info(objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException ignored) {
            ACCESS_LOG.warn("access event serialization failed trace_id={}", event.get("trace_id"));
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) return forwarded.split(",")[0].trim();
        String real = request.getHeader("X-Real-IP");
        return StringUtils.hasText(real) ? real.trim() : request.getRemoteAddr();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
