package com.uestcfir.gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import reactor.core.publisher.Mono;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/** Per-process token buckets; peer addresses never come from client forwarding headers. */
@Component
public class ClientRateLimitFilter implements GlobalFilter, Ordered {
    private final Map<String, Bucket> buckets = new HashMap<>();
    private final int loginRate, queryRate, controlRate, capacity;
    private final LongSupplier clock;

    @Autowired
    public ClientRateLimitFilter(@Value("${gateway.rate-limit.login-rps:5}") int loginRate,
            @Value("${gateway.rate-limit.query-rps:20}") int queryRate,
            @Value("${gateway.rate-limit.control-rps:5}") int controlRate,
            @Value("${gateway.rate-limit.burst:5}") int capacity) {
        this(loginRate, queryRate, controlRate, capacity, System::nanoTime);
    }

    ClientRateLimitFilter(int loginRate, int queryRate, int controlRate, int capacity, LongSupplier clock) {
        if (loginRate < 1 || queryRate < 1 || controlRate < 1 || capacity < 1) throw new IllegalArgumentException("Invalid rate limits");
        this.loginRate = loginRate;
        this.queryRate = queryRate;
        this.controlRate = controlRate;
        this.capacity = capacity;
        this.clock = clock;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.equals("/iot/ws/device") || exchange.getRequest().getMethod().name().equals("OPTIONS")) return chain.filter(exchange);
        String policy = path.startsWith("/user/login") || path.startsWith("/approver/login")
                || path.endsWith("/sendcode") || path.endsWith("/register") ? "login"
                : path.matches("/iot/devices/[^/]+/commands") ? "control" : "query";
        int rate = policy.equals("login") ? loginRate : policy.equals("control") ? controlRate : queryRate;
        String peer = exchange.getRequest().getRemoteAddress() == null ? "unknown"
                : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        if (!acquire(policy + ":" + peer, rate)) {
            exchange.getResponse().getHeaders().set("Retry-After", "1");
            return GatewayResponses.error(exchange, 429, "Request rate exceeded");
        }
        return chain.filter(exchange);
    }

    private synchronized boolean acquire(String key, int rate) {
        long now = clock.getAsLong();
        Bucket bucket = buckets.get(key);
        if (bucket == null) {
            if (buckets.size() >= 10000) buckets.entrySet().removeIf(e -> now - e.getValue().last > 60_000_000_000L);
            if (buckets.size() >= 10000) return false;
            bucket = new Bucket(capacity, now);
            buckets.put(key, bucket);
        }
        bucket.tokens = Math.min(capacity, bucket.tokens + Math.max(0, now - bucket.last) / 1_000_000_000D * rate);
        bucket.last = now;
        if (bucket.tokens < 1) return false;
        bucket.tokens -= 1;
        return true;
    }

    @Override
    public int getOrder() { return Ordered.HIGHEST_PRECEDENCE + 5; }

    private static class Bucket {
        double tokens;
        long last;
        Bucket(double tokens, long last) { this.tokens = tokens; this.last = last; }
    }
}
