package com.uestcfir.logservice;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in: mvn -pl log-service -Dtest=ClickHouseQueryIT test (local schema required). */
class ClickHouseQueryIT {
    @Test
    void queryCollectedEventsUsingRealClickHouseDriver() {
        var source = new DriverManagerDataSource("jdbc:clickhouse://127.0.0.1:8123/default?compress=0", "default", "");
        source.setDriverClassName("com.clickhouse.jdbc.ClickHouseDriver");
        var service = new LogQueryService(new JdbcTemplate(source));
        var query = new LogQuery(Instant.now().minusSeconds(86400), Instant.now().plusSeconds(60),
                "topbiz-gateway", null, null, null, null, null, 1, 20);
        var result = service.query(query);
        assertTrue(((Number) result.get("total")).longValue() >= 50);
        assertEquals(20, ((java.util.List<?>) result.get("list")).size());
        assertTrue(((Number) result.get("queryDurationMs")).longValue() >= 0);
    }
}
