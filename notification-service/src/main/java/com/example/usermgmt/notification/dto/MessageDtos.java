package com.example.usermgmt.notification.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

public final class MessageDtos {

    private MessageDtos() {
    }

    @Data
    public static class Recipient {
        @NotBlank(message = "recipient.type is required")
        private String type;

        @NotBlank(message = "recipient.value is required")
        private String value;
    }

    @Data
    public static class Strategy {
        private Integer priority;
        private Integer retry;
    }

    @Data
    public static class SendRequest {
        private String requestId;
        private String policyId;

        private String businessCode;

        private String templateId;
        private String templateCode;

        @Valid
        private Recipient recipient;

        private JsonNode params;
        private String channel;
        private String scheduleAt;

        @Valid
        private Strategy strategy;
    }

    @Data
    public static class BatchSendRequest {
        private String requestId;
        private String policyId;

        private String businessCode;

        private String templateId;
        private String templateCode;

        @Valid
        @NotEmpty(message = "recipients is required")
        private List<Recipient> recipients;

        private JsonNode params;
        private String channel;
        private String scheduleAt;

        @Valid
        private Strategy strategy;
    }

    @Data
    public static class SendResponse {
        private String messageId;
        private String batchId;
        private String status;
        private LocalDateTime plannedAt;
        private Integer retryCount;
    }

    @Data
    public static class BatchSendResponse {
        private String batchId;
        private Integer total;
        private Integer success;
        private Integer failed;
        private String status;
        private List<String> messageIds;
    }

    @Data
    public static class MessageRecord {
        private String messageId;
        private String batchId;
        private String requestId;
        private String policyId;
        private String businessCode;
        private String templateId;
        private String templateCode;
        private String channel;
        private String provider;
        private String recipientType;
        private String recipient;
        private String status;
        private Integer retryCount;
        private Integer maxRetry;
        private String title;
        private String content;
        private JsonNode params;
        private JsonNode templateSnapshot;
        private String response;
        private String failureReason;
        private LocalDateTime plannedAt;
        private LocalDateTime sentAt;
        private LocalDateTime nextRetryAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class ListResponse {
        private Long total;
        private Integer page;
        private Integer size;
        private List<MessageRecord> records;
    }

    @Data
    public static class BatchDetailResponse {
        private String batchId;
        private Integer total;
        private Integer success;
        private Integer failed;
        private String status;
        private List<MessageRecord> records;
    }
}
