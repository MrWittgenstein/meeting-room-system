package com.uestcfir.service;

import com.uestcfir.pojo.dto.IotTelemetryRequest;
import com.uestcfir.pojo.entity.IotDevice;
import com.uestcfir.pojo.entity.IotRoomStatus;
import com.uestcfir.pojo.entity.IotSensorRecord;

import java.util.List;

public interface IotTelemetryService {
    IotSensorRecord saveTelemetry(IotTelemetryRequest request, String payloadJson);

    IotSensorRecord getLatestByRoomId(Integer roomId);

    List<IotSensorRecord> getRecentByRoomId(Integer roomId, Integer limit);

    IotSensorRecord getLatestByDeviceId(String deviceId);

    IotRoomStatus getRoomStatus(Integer roomId);

    IotDevice getDevice(String deviceId);
}
