package com.uestcfir.pojo.dto;

import lombok.Data;

import java.util.Map;

@Data
public class IotDeviceCommandRequest {
    private String command;
    private String target;
    private Object value;
    private Double angle;
    private Map<String, Object> params;
}
