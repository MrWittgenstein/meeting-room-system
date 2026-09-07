package com.uestcfir.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SessionAuthenticationFilter implements GlobalFilter, Ordered {
    private static final String SESSION_KEY_PREFIX = "zongshe:session:";
    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/user/login", "/user/register", "/user/sendcode", "/user/changepassword",
            "/approver/login", "/approver/register", "/approver/sendcode",
            "/swagger-ui", "/v3/api-docs", "/assets", "/styles", "/scripts", "/vendor",
            "/iot/ws/device", "/actuator/health", "/error", "/"
    );

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isPublic(path)) {
            return chain.filter(exchange);
        }
        String sessionId = resolveSessionId(exchange);
        if (sessionId == null || sessionId.isBlank()) {
            return unauthorized(exchange, "Missing or expired session");
        }
        return redisTemplate.opsForValue().get(SESSION_KEY_PREFIX + sessionId)
                .flatMap(json -> forwardIdentity(exchange, chain, sessionId, json))
                .switchIfEmpty(unauthorized(exchange, "Session not found or expired"));
    }

    private Mono<Void> forwardIdentity(ServerWebExchange exchange, GatewayFilterChain chain,
                                       String sessionId, String json) {
        try {
            JsonNode session = objectMapper.readTree(json);
            String permissions = objectMapper.writeValueAsString(session.path("permissions"));
            ServerWebExchange authenticatedExchange = exchange.mutate()
                    .request(exchange.getRequest().mutate()
                            .headers(headers -> {
                                headers.set("SessionId", sessionId);
                                headers.set("X-Authenticated-User-Id", session.path("userId").asText());
                                headers.set("X-Authenticated-Role", session.path("role").asText());
                                headers.set("X-Authenticated-Permissions", permissions);
                            })
                            .build())
                    .build();
            return chain.filter(authenticatedExchange);
        } catch (Exception exception) {
            return unauthorized(exchange, "Invalid session data");
        }
    }

    private String resolveSessionId(ServerWebExchange exchange) {
        String sessionId = exchange.getRequest().getHeaders().getFirst("SessionId");
        if (sessionId != null && !sessionId.isBlank()) {
            return sessionId.trim();
        }
        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        return exchange.getRequest().getHeaders().getFirst("X-Session-Id");
    }

    private boolean isPublic(String path) {
        if ("/".equals(path) || "/index.html".equals(path)) {
            return true;
        }
        return PUBLIC_PATH_PREFIXES.stream()
                .filter(prefix -> !"/".equals(prefix))
                .anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = ("{\"code\":401,\"message\":\"" + message + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
