package com.uestcfir.logservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LogServiceTest {
    private LogQuery query(Instant start, Instant end, Integer page) {
        return new LogQuery(start, end, null, null, null, null, null, null, page, 20);
    }

    @Test
    void unboundedAndReversedQueriesAreRejectedBeforeDatabaseAccess() {
        var jdbc = mock(JdbcTemplate.class);
        var service = new LogQueryService(jdbc);
        Instant start = Instant.parse("2026-09-23T00:00:00Z");
        assertThrows(ResponseStatusException.class, () -> service.query(query(start,start.minusSeconds(1),1)));
        assertThrows(ResponseStatusException.class, () -> service.query(query(start,start.plusSeconds(32*86400),1)));
        assertThrows(ResponseStatusException.class, () -> service.query(query(start,start.plusSeconds(60),-1)));
        verify(jdbc).setQueryTimeout(10);
        verifyNoMoreInteractions(jdbc);
    }

    @Test
    @SuppressWarnings("unchecked")
    void ordinaryUserCannotReadLogsAndAdministratorCan() {
        var service = mock(LogQueryService.class);
        var redis = mock(StringRedisTemplate.class);
        ValueOperations<String,String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        var controller = new LogController(service, redis, new ObjectMapper());
        assertEquals(401, assertThrows(ResponseStatusException.class, () -> controller.query(null, null)).getStatusCode().value());
        when(values.get("zongshe:session:ordinary")).thenReturn("{\"userId\":18,\"role\":\"user\"}");
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> controller.query("ordinary", null)).getStatusCode().value());
        verifyNoInteractions(service);
        when(values.get("zongshe:session:admin")).thenReturn("{\"userId\":10,\"role\":\"super_admin\"}");
        var query = query(Instant.now(),Instant.now().plusSeconds(60),1);
        when(service.query(query)).thenReturn(java.util.Map.of("total",0));
        assertEquals(1,controller.query("admin",query).get("code"));
    }
}
