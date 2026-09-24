package com.uestcfir.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceLoggingFilter implements GlobalFilter {
    private static final Logger ACCESS_LOG = org.slf4j.LoggerFactory.getLogger("ACCESS_LOG");
    private final ObjectMapper objectMapper;

    public TraceLoggingFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = UUID.randomUUID().toString();
        ServerWebExchange traced = exchange.mutate().request(exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove("X-Trace-Id");
                    headers.remove("Trace-Id");
                    headers.remove("X-Request-Id");
                    headers.remove("X-Authenticated-User-Id");
                    headers.remove("X-Authenticated-Role");
                    headers.remove("X-Authenticated-Permissions");
                    headers.remove("X-User-Id");
                    headers.remove("User-Id");
                    headers.remove("X-Forwarded-For");
                    headers.remove("X-Real-IP");
                    if (exchange.getRequest().getRemoteAddress() != null) {
                        headers.set("X-Forwarded-For", exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
                    }
                    headers.set("X-Trace-Id", traceId);
                }).build()).response(exchange.getResponse()).build();
        traced.getResponse().getHeaders().set("X-Trace-Id", traceId);
        traced.getResponse().beforeCommit(() -> {
            traced.getResponse().getHeaders().set("X-Trace-Id", traceId);
            return Mono.empty();
        });
        long started = System.nanoTime();
        return chain.filter(traced)
                .onErrorResume(error -> {
                    if (traced.getResponse().isCommitted()) return Mono.error(error);
                    boolean timeout = error instanceof java.util.concurrent.TimeoutException
                            || error instanceof org.springframework.web.server.ResponseStatusException statusError
                            && statusError.getStatusCode().value() == 504;
                    return GatewayResponses.error(traced, timeout ? 504 : 503,
                            timeout ? "Upstream request timed out" : "Upstream service unavailable");
                })
                .doFinally(signal -> writeEvent(traced, traceId, started));
    }

    private void writeEvent(ServerWebExchange exchange, String traceId, long started) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("event_id", UUID.randomUUID().toString());
        event.put("timestamp", Instant.now().toString());
        event.put("microservice", "topbiz-gateway");
        event.put("api_endpoint", exchange.getRequest().getURI().getPath());
        event.put("http_method", exchange.getRequest().getMethod().name());
        HttpStatusCode status = exchange.getResponse().getStatusCode();
        event.put("status_code", status == null ? 200 : status.value());
        event.put("business_code", exchange.getResponse().getHeaders().getFirst("X-Business-Code") == null
                ? (status != null && status.isError() ? status.value() : 1)
                : Integer.parseInt(exchange.getResponse().getHeaders().getFirst("X-Business-Code")));
        event.put("latency_ms", Math.max(0L, (System.nanoTime() - started) / 1_000_000L));
        event.put("trace_id", traceId);
        event.put("user_id", exchange.getAttributeOrDefault("authenticatedUserId", ""));
        event.put("client_ip", exchange.getRequest().getRemoteAddress() == null ? "" : String.valueOf(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()));
        event.put("user_agent", safe(exchange.getRequest().getHeaders().getFirst("User-Agent")));
        try {
            ACCESS_LOG.info(objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException ignored) {
            ACCESS_LOG.warn("gateway access event serialization failed trace_id={}", traceId);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
