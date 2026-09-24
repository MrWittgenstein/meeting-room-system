package com.uestcfir.logservice;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class LogQueryService {
    private final JdbcTemplate jdbc;

    public LogQueryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        jdbc.setQueryTimeout(10);
    }

    public Map<String, Object> query(LogQuery query) {
        if (query.startTime() == null || query.endTime() == null || !query.startTime().isBefore(query.endTime())
                || Duration.between(query.startTime(), query.endTime()).compareTo(Duration.ofDays(31)) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query range must be positive and at most 31 days");
        }
        int page = query.page() == null ? 1 : query.page();
        int size = query.pageSize() == null ? 20 : query.pageSize();
        if (page < 1 || page > 10000 || size < 1 || size > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination");
        }
        StringBuilder where = new StringBuilder("timestamp >= parseDateTime64BestEffort(?, 3, 'UTC') AND timestamp < parseDateTime64BestEffort(?, 3, 'UTC')");
        List<Object> parameters = new ArrayList<>(List.of(query.startTime().toString(), query.endTime().toString()));
        add(where, parameters, "microservice", query.microservice());
        add(where, parameters, "trace_id", query.traceId());
        add(where, parameters, "user_id", query.userId());
        add(where, parameters, "device_id", query.deviceId());
        add(where, parameters, "command_id", query.commandId());
        add(where, parameters, "status_code", query.statusCode());
        String from = " FROM meetingroom_logs.access_events FINAL WHERE " + where;
        long started = System.nanoTime();
        Long total = jdbc.queryForObject("SELECT count()" + from, Long.class, parameters.toArray());
        parameters.add(size);
        parameters.add((long) (page - 1) * size);
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT *" + from + " ORDER BY timestamp DESC, event_id LIMIT ? OFFSET ?", parameters.toArray());
        return Map.of("page", page, "pageSize", size, "total", total == null ? 0 : total, "list", rows,
                "queryDurationMs", (System.nanoTime() - started) / 1_000_000);
    }

    private void add(StringBuilder where, List<Object> parameters, String column, Object value) {
        if (value == null || value instanceof String text && text.isBlank()) return;
        where.append(" AND ").append(column).append(" = ?");
        parameters.add(value);
    }
}
