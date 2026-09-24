package com.uestcfir.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GatewayFiltersTest {
    @Test
    void twentyConcurrentRequestsAllowOnlyFiveWithoutRefill() {
        var limiter = new ClientRateLimitFilter(5, 5, 5, 5, () -> 0L);
        AtomicInteger allowed = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        IntStream.range(0, 20).parallel().forEach(i -> {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/user/info"));
            limiter.filter(exchange, e -> { allowed.incrementAndGet(); return Mono.empty(); }).block();
            if (exchange.getResponse().getStatusCode() != null && exchange.getResponse().getStatusCode().value() == 429) rejected.incrementAndGet();
        });
        assertEquals(5, allowed.get());
        assertEquals(15, rejected.get());
    }

    @Test
    @SuppressWarnings("unchecked")
    void validSessionDoesNotFallThroughToUnauthorizedAfterEmptyCompletion() {
        var redis = mock(ReactiveStringRedisTemplate.class);
        ReactiveValueOperations<String, String> values = mock(ReactiveValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("zongshe:session:valid")).thenReturn(Mono.just("{\"userId\":18,\"role\":\"user\",\"permissions\":[]}"));
        var filter = new SessionAuthenticationFilter(redis, new ObjectMapper());
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/user/info").header("SessionId", "valid"));
        AtomicInteger forwarded = new AtomicInteger();
        filter.filter(exchange, e -> { forwarded.incrementAndGet(); return Mono.empty(); }).block();
        assertEquals(1, forwarded.get());
        assertNotEquals(401, exchange.getResponse().getStatusCode() == null ? 200 : exchange.getResponse().getStatusCode().value());
    }

    @Test
    void publicLoginStripsForgedIdentityAndReplacesExternalTrace() {
        var filter = new TraceLoggingFilter(new ObjectMapper());
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/user/login")
                .header("X-Authenticated-User-Id", "999").header("X-Trace-Id", "external"));
        filter.filter(exchange, e -> {
            assertNull(e.getRequest().getHeaders().getFirst("X-Authenticated-User-Id"));
            assertNotEquals("external", e.getRequest().getHeaders().getFirst("X-Trace-Id"));
            return Mono.empty();
        }).block();
        assertNotNull(exchange.getResponse().getHeaders().getFirst("X-Trace-Id"));
    }

    @Test
    void upstreamFailureHasStructuredResponseAndTrace() {
        var filter = new TraceLoggingFilter(new ObjectMapper());
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/user/info"));
        filter.filter(exchange, e -> Mono.error(new java.net.ConnectException())).block();
        assertEquals(503, exchange.getResponse().getStatusCode().value());
        assertTrue(exchange.getResponse().getBodyAsString().block().contains("503"));
        assertNotNull(exchange.getResponse().getHeaders().getFirst("X-Trace-Id"));
    }

    @Test
    void loginPrefixDoesNotMakeUnrelatedPathPublic() {
        var filter = new SessionAuthenticationFilter(mock(ReactiveStringRedisTemplate.class), new ObjectMapper());
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/user/login-admin"));
        filter.filter(exchange, e -> { fail("must not forward"); return Mono.empty(); }).block();
        assertEquals(401, exchange.getResponse().getStatusCode().value());
    }
}
