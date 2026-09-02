package com.uestcfir.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IotDeviceCommandResultVo {
    private String commandId;
    private String deviceId;
    private String status;
    private String message;
    private Map<String, Object> ack;
}
