package com.uestcfir.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IotRealtimeMessageVo {
    private IotTelemetryVo current;
    private List<IotTelemetryVo> history;
}
