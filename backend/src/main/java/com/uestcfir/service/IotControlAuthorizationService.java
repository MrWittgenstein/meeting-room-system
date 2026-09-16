package com.uestcfir.service;

import com.uestcfir.auth.CurrentUserContext;
import com.uestcfir.auth.RolePermissionRegistry;
import com.uestcfir.auth.SessionUser;
import com.uestcfir.enumeration.user.UserType;
import com.uestcfir.exception.BusinessException;
import com.uestcfir.mapper.IotDeviceMapper;
import com.uestcfir.mapper.ReservationMapper;
import com.uestcfir.pojo.entity.IotDevice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Resource and time scoped authorization for IoT device commands.
 */
@Service
@RequiredArgsConstructor
public class IotControlAuthorizationService {
    private final IotDeviceMapper iotDeviceMapper;
    private final ReservationMapper reservationMapper;

    public IotDevice requireCanControl(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new BusinessException("deviceId can not be empty");
        }

        IotDevice device = iotDeviceMapper.findByDeviceId(deviceId.trim());
        if (device == null) {
            throw new BusinessException("IoT device does not exist, deviceId=" + deviceId);
        }

        SessionUser current = CurrentUserContext.requireUser();
        if (isGlobalController(current)) {
            return device;
        }

        Integer roomId = device.getRoomId();
        boolean activeReservation = roomId != null
                && reservationMapper.existsActiveReservation(
                current.getUserId(), roomId, LocalDate.now(), LocalTime.now());
        if (!activeReservation) {
            throw new BusinessException("当前没有该会议室的有效预约，无法控制设备");
        }
        return device;
    }

    private boolean isGlobalController(SessionUser user) {
        Integer userType = user.getUserType();
        boolean administrator = UserType.APPROVER.getCode().equals(userType)
                || UserType.ADMIN.getCode().equals(userType)
                || "room_admin".equalsIgnoreCase(user.getRole())
                || "super_admin".equalsIgnoreCase(user.getRole());
        return administrator && user.getPermissions() != null
                && user.getPermissions().contains(RolePermissionRegistry.IOT_CONTROL);
    }
}
