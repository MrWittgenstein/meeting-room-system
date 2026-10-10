package com.example.usermgmt.notification.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TemplateEntity {

    private Long id;
    private String templateCode;
    private String templateName;
    private Long carrierId;
    private String channelCode;
    private String providerTemplateId;
    private String content;
    private String variableDefinitions;
    private Integer version;
    private String remark;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
