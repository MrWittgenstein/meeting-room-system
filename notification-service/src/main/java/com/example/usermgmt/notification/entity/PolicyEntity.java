package com.example.usermgmt.notification.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PolicyEntity {

    private Long id;
    private String policyName;
    private Long templateId;
    private String templateCode;
    private Long carrierId;
    private String channelType;
    private String businessCode;
    private String dynamicCondition;
    private String sendMode;
    private LocalDateTime sendTime;
    private String recurringRule;
    private LocalDateTime recurringEndTime;
    private Integer priority;
    private Integer maxRetry;
    private String remark;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
