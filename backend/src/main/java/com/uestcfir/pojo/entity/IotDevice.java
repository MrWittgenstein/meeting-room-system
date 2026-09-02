package com.uestcfir.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IotDevice {
    private String deviceId;
    private String roomCode;
    private Integer roomId;
    private String deviceName;
    private String deviceType;
    private String protocol;
    private String onlineStatus;
    private LocalDateTime lastSeenAt;
    private LocalDateTime lastReportedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
