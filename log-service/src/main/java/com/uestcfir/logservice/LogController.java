package com.uestcfir.logservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/logs")
public class LogController {
    private final LogQueryService service;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public LogController(LogQueryService service, StringRedisTemplate redis, ObjectMapper mapper) {
        this.service = service;
        this.redis = redis;
        this.mapper = mapper;
    }

    @PostMapping("/query")
    public Map<String, Object> query(@RequestHeader(value = "SessionId", required = false) String sessionId,
                                    @Valid @RequestBody LogQuery query) {
        if (sessionId == null || sessionId.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        String json = redis.opsForValue().get("zongshe:session:" + sessionId.trim());
        if (json == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        JsonNode user;
        try {
            user = mapper.readTree(json);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        String role = user.path("role").asText();
        if ((!"room_admin".equals(role) && !"super_admin".equals(role)) || user.path("userId").asInt() <= 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return Map.of("code", 1, "message", "success", "data", service.query(query));
    }
}
