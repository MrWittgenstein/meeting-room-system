package com.uestcfir.controller.iot;

import com.uestcfir.pojo.entity.Result;
import com.uestcfir.pojo.dto.IotDeviceCommandRequest;
import com.uestcfir.pojo.vo.IotTelemetryVo;
import com.uestcfir.service.IotTelemetryService;
import com.uestcfir.websocket.IotDeviceWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/iot")
public class IotTelemetryController {
    @Autowired
    private IotTelemetryService iotTelemetryService;

    @Autowired
    private IotDeviceWebSocketHandler iotDeviceWebSocketHandler;

    @GetMapping("/rooms/{roomId}/latest")
    public Result getLatestByRoom(@PathVariable Integer roomId) {
        return Result.success(IotTelemetryVo.fromEntity(iotTelemetryService.getLatestByRoomId(roomId)));
    }

    @GetMapping("/rooms/{roomId}/status")
    public Result getRoomStatus(@PathVariable Integer roomId) {
        return Result.success(IotTelemetryVo.fromRoomStatus(iotTelemetryService.getRoomStatus(roomId)));
    }

    @GetMapping("/rooms/{roomId}/history")
    public Result getHistoryByRoom(@PathVariable Integer roomId,
                                   @RequestParam(defaultValue = "20") Integer limit) {
        List<IotTelemetryVo> history = iotTelemetryService.getRecentByRoomId(roomId, limit)
                .stream()
                .map(IotTelemetryVo::fromEntity)
                .toList();
        return Result.success(history);
    }

    @GetMapping("/rooms/{roomId}/last-seven")
    public Result getLastSevenByRoom(@PathVariable Integer roomId) {
        List<IotTelemetryVo> lastSeven = iotTelemetryService.getRecentByRoomId(roomId, 7)
                .stream()
                .map(IotTelemetryVo::fromEntity)
                .toList();
        return Result.success(lastSeven);
    }

    @GetMapping("/devices/{deviceId}/latest")
    public Result getLatestByDevice(@PathVariable String deviceId) {
        return Result.success(IotTelemetryVo.fromEntity(iotTelemetryService.getLatestByDeviceId(deviceId)));
    }

    @GetMapping("/devices/{deviceId}")
    public Result getDevice(@PathVariable String deviceId) {
        return Result.success(iotTelemetryService.getDevice(deviceId));
    }

    @PostMapping("/devices/{deviceId}/commands")
    public Result sendDeviceCommand(@PathVariable String deviceId,
                                    @RequestBody IotDeviceCommandRequest request) {
        return Result.success(iotDeviceWebSocketHandler.sendCommand(deviceId, request));
    }
}
