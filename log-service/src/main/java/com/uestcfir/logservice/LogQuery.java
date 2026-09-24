package com.uestcfir.logservice;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record LogQuery(
        @NotNull @JsonAlias("start_time") Instant startTime,
        @NotNull @JsonAlias("end_time") Instant endTime,
        String microservice,
        @JsonAlias("trace_id") String traceId,
        @JsonAlias("user_id") String userId,
        @JsonAlias("device_id") String deviceId,
        @JsonAlias("command_id") String commandId,
        @JsonAlias("status_code") Integer statusCode,
        @Min(1) @Max(10000) Integer page,
        @Min(1) @Max(200) @JsonAlias("page_size") Integer pageSize) { }
