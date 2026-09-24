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
import java.time.Clock;
import java.time.LocalDateTime;
import com.uestcfir.mapper.MeetingroomMapper;
import com.uestcfir.logging.CommandAudit;

/**
 * Resource and time scoped authorization for IoT device commands.
 */
@Service
@RequiredArgsConstructor
public class IotControlAuthorizationService {
    private final IotDeviceMapper iotDeviceMapper;
    private final ReservationMapper reservationMapper;
    private final MeetingroomMapper meetingroomMapper;
    private final Clock clock;

    public IotDevice requireCanControl(String deviceId) {
        CommandAudit.put("deviceId", deviceId);
        CommandAudit.put("outcome", "permission_denied");
        SessionUser current = CurrentUserContext.requireUser();
        if (deviceId == null || deviceId.isBlank()) {
            throw new BusinessException("deviceId can not be empty");
        }

        IotDevice device = iotDeviceMapper.findByDeviceId(deviceId.trim());
        if (device == null) {
            throw new BusinessException(403, "IoT device is not registered");
        }
        Integer roomId = device.getRoomId();
        CommandAudit.put("roomId", roomId);
        if (roomId == null || meetingroomMapper.getMeetingroomById(roomId) == null) {
            throw new BusinessException(403, "IoT device has no valid room binding");
        }
        if (isGlobalController(current)) {
            CommandAudit.put("outcome", "authorized");
            return device;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        boolean activeReservation = roomId != null
                && reservationMapper.existsActiveReservation(
                current.getUserId(), roomId, now.toLocalDate(), now.toLocalTime());
        if (!activeReservation) {
            throw new BusinessException(403, "当前没有该会议室的有效预约，无法控制设备");
        }
        CommandAudit.put("outcome", "authorized");
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
