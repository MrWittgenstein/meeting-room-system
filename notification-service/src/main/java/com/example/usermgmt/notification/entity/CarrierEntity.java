package com.example.usermgmt.notification.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CarrierEntity {

    private Long id;
    private String channelName;
    private String channelType;
    private String channelProvider;
    private String configJson;
    private Integer priority;
    private Integer weight;
    private Integer maxRetry;
    private String remark;
    private String status;
    private Boolean defaultCarrier;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
