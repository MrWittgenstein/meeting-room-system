package com.example.usermgmt.notification.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

public final class TemplateDtos {

    private TemplateDtos() {
    }

    @Data
    public static class CreateRequest {
        @NotBlank(message = "businessCode is required")
        private String businessCode;

        private String templateCode;

        @NotBlank(message = "name is required")
        private String name;

        @NotBlank(message = "channel is required")
        private String channel;

        @NotNull(message = "content is required")
        private JsonNode content;

        private JsonNode paramsSchema;
        private String status;
        private String createdBy;
    }

    @Data
    public static class UpdateRequest {
        private String businessCode;
        private String templateCode;
        private String name;
        private String channel;
        private JsonNode content;
        private JsonNode paramsSchema;
        private String status;
        private String updatedBy;
        private String reason;
    }

    @Data
    public static class PublishRequest {
        private Integer version;
    }

    @Data
    public static class RollbackRequest {
        @NotNull(message = "targetVersion is required")
        private Integer targetVersion;

        private String reason;
        private String operator;
    }

    @Data
    public static class PreviewRequest {
        @NotBlank(message = "templateId is required")
        private String templateId;

        private Integer version;
        private String channel;
        private JsonNode params;
    }

    @Data
    public static class Record {
        private String templateId;
        private String templateCode;
        private String businessCode;
        private String name;
        private String channel;
        private JsonNode content;
        private JsonNode paramsSchema;
        private Integer version;
        private String status;
        private String createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class ListResponse {
        private Long total;
        private Integer page;
        private Integer size;
        private List<Record> records;
    }

    @Data
    public static class VersionRecord {
        private String templateId;
        private Integer version;
        private String templateCode;
        private String businessCode;
        private String name;
        private String channel;
        private JsonNode content;
        private JsonNode paramsSchema;
        private String status;
        private String changeReason;
        private String createdBy;
        private LocalDateTime createdAt;
    }

    @Data
    public static class PreviewResponse {
        private String title;
        private String body;
        private String rendered;
        private String channel;
    }
}
