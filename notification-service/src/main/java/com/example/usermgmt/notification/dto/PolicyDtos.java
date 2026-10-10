package com.example.usermgmt.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

public final class PolicyDtos {

    private PolicyDtos() {
    }

    @Data
    public static class CreateRequest {
        @NotBlank(message = "policyName is required")
        private String policyName;

        private String templateId;
        private String templateCode;
        private String carrierId;
        private String channelType;

        @NotBlank(message = "businessCode is required")
        private String businessCode;

        @NotBlank(message = "dynamicCondition is required")
        private String dynamicCondition;

        @NotBlank(message = "sendMode is required")
        private String sendMode;

        private String sendTime;
        private String recurringRule;
        private String recurringEndTime;
        private String nextFireTime;
        private Integer priority;
        private Integer maxRetry;
        private String remark;
        private String status;
        private String createdBy;
    }

    @Data
    public static class UpdateRequest {
        private String policyName;
        private String templateId;
        private String templateCode;
        private String carrierId;
        private String channelType;
        private String businessCode;
        private String dynamicCondition;
        private String sendMode;
        private String sendTime;
        private String recurringRule;
        private String recurringEndTime;
        private String nextFireTime;
        private Integer priority;
        private Integer maxRetry;
        private String remark;
        private String status;
    }

    @Data
    public static class Record {
        private String policyId;
        private String policyName;
        private String templateId;
        private String templateCode;
        private String carrierId;
        private String channelType;
        private String businessCode;
        private String dynamicCondition;
        private String sendMode;
        private LocalDateTime sendTime;
        private String recurringRule;
        private LocalDateTime recurringEndTime;
        private LocalDateTime nextFireTime;
        private LocalDateTime lastFireTime;
        private Integer priority;
        private Integer maxRetry;
        private String remark;
        private String status;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }

    @Data
    public static class ListResponse {
        private Long total;
        private Integer page;
        private Integer size;
        private List<Record> records;
    }
}
