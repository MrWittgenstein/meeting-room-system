package com.example.usermgmt.notification.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Data;

public final class ChannelDtos {

    private ChannelDtos() {
    }

    @Data
    public static class ConfigureRequest {
        private String channelName;

        @NotBlank(message = "provider is required")
        private String provider;

        @NotNull(message = "config is required")
        private JsonNode config;

        private Integer priority;
        private Integer weight;
        private Integer maxRetry;
        private String remark;
        private String status;
        private Boolean defaultCarrier;
    }

    @Data
    public static class ConfigureResponse {
        private String channelId;
        private String channelType;
        private String channelName;
        private String provider;
        private JsonNode config;
        private Integer priority;
        private Integer weight;
        private Integer maxRetry;
        private String status;
        private String health;
        private Boolean defaultCarrier;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class SwitchRequest {
        @NotBlank(message = "provider is required")
        private String provider;

        private String reason;
    }

    @Data
    public static class RuntimeStatus {
        private String channelId;
        private String channelType;
        private String channelName;
        private String provider;
        private JsonNode config;
        private String status;
        private String health;
        private Integer priority;
        private Integer weight;
        private Integer maxRetry;
        private Boolean defaultCarrier;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class StatusResponse {
        private Map<String, RuntimeStatus> channels;
        private List<ConfigureResponse> carriers;
    }
}
