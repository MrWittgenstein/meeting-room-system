package com.example.usermgmt.notification.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MessageEntity {

    private Long id;
    private Long policyId;
    private Long templateId;
    private String templateCode;
    private Long carrierId;
    private String channelType;
    private String businessCode;
    private Long receiverUid;
    private String receiverContact;
    private String params;
    private String templateSnapshot;
    private LocalDateTime sendTimePlanned;
    private LocalDateTime sendTimeActual;
    private LocalDateTime readTime;
    private String response;
    private String failureReason;
    private Integer retryCount;
    private String remark;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
