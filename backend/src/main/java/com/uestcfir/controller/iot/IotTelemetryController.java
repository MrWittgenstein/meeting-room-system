package com.uestcfir.controller.iot;

import com.uestcfir.auth.CurrentUserContext;
import com.uestcfir.auth.RolePermissionRegistry;
import com.uestcfir.pojo.entity.Result;
import com.uestcfir.pojo.dto.IotDeviceCommandRequest;
import com.uestcfir.pojo.vo.IotTelemetryVo;
import com.uestcfir.pojo.vo.IotControlRoomVo;
import com.uestcfir.service.IotTelemetryService;
import com.uestcfir.service.IotControlAuthorizationService;
import com.uestcfir.mapper.IotDeviceMapper;
import com.uestcfir.exception.BusinessException;
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
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/iot")
public class IotTelemetryController {
    @Autowired
    private IotTelemetryService iotTelemetryService;

    @Autowired
    private IotDeviceWebSocketHandler iotDeviceWebSocketHandler;

    @Autowired
    private IotControlAuthorizationService iotControlAuthorizationService;

    @Autowired
    private IotDeviceMapper iotDeviceMapper;

    @GetMapping("/control-rooms")
    public Result getControlRooms() {
        CurrentUserContext.requirePermission(RolePermissionRegistry.ROOM_READ);
        List<IotControlRoomVo> rooms = iotDeviceMapper.findControlRooms();
        return Result.success(rooms);
    }

    @GetMapping("/rooms/{roomId}/latest")
    public Result getLatestByRoom(@PathVariable Integer roomId) {
        CurrentUserContext.requirePermission(RolePermissionRegistry.IOT_READ);
        return Result.success(IotTelemetryVo.fromEntity(iotTelemetryService.getLatestByRoomId(roomId)));
    }

    @GetMapping("/rooms/{roomId}/status")
    public Result getRoomStatus(@PathVariable Integer roomId) {
        CurrentUserContext.requirePermission(RolePermissionRegistry.IOT_READ);
        return Result.success(IotTelemetryVo.fromRoomStatus(iotTelemetryService.getRoomStatus(roomId)));
    }

    @GetMapping("/rooms/{roomId}/history")
    public Result getHistoryByRoom(@PathVariable Integer roomId,
                                   @RequestParam(defaultValue = "20") Integer limit) {
        CurrentUserContext.requirePermission(RolePermissionRegistry.IOT_READ);
        List<IotTelemetryVo> history = iotTelemetryService.getRecentByRoomId(roomId, limit)
                .stream()
                .map(IotTelemetryVo::fromEntity)
                .toList();
        return Result.success(history);
    }

    @GetMapping("/rooms/{roomId}/last-seven")
    public Result getLastSevenByRoom(@PathVariable Integer roomId) {
        CurrentUserContext.requirePermission(RolePermissionRegistry.IOT_READ);
        List<IotTelemetryVo> lastSeven = iotTelemetryService.getRecentByRoomId(roomId, 7)
                .stream()
                .map(IotTelemetryVo::fromEntity)
                .toList();
        return Result.success(lastSeven);
    }

    @GetMapping("/devices/{deviceId}/latest")
    public Result getLatestByDevice(@PathVariable String deviceId) {
        CurrentUserContext.requirePermission(RolePermissionRegistry.IOT_READ);
        return Result.success(IotTelemetryVo.fromEntity(iotTelemetryService.getLatestByDeviceId(deviceId)));
    }

    @GetMapping("/devices/{deviceId}")
    public Result getDevice(@PathVariable String deviceId) {
        CurrentUserContext.requirePermission(RolePermissionRegistry.IOT_READ);
        return Result.success(iotTelemetryService.getDevice(deviceId));
    }

    @PostMapping("/devices/{deviceId}/commands")
    public Result sendDeviceCommand(@PathVariable String deviceId,
                                    @RequestBody IotDeviceCommandRequest request) {
        iotControlAuthorizationService.requireCanControl(deviceId);
        return Result.success(iotDeviceWebSocketHandler.sendCommand(deviceId, request));
    }

    @GetMapping("/devices/{deviceId}/control-access")
    public Result getControlAccess(@PathVariable String deviceId) {
        Map<String, Object> data = new LinkedHashMap<>();
        try {
            var device = iotControlAuthorizationService.requireCanControl(deviceId);
            data.put("allowed", true);
            data.put("roomId", device.getRoomId());
            data.put("deviceId", device.getDeviceId());
        } catch (BusinessException e) {
            data.put("allowed", false);
            data.put("message", e.getMessage());
        }
        return Result.success(data);
    }
}
