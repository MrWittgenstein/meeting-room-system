package com.uestcfir.service;

import com.uestcfir.auth.CurrentUserContext;
import com.uestcfir.auth.RolePermissionRegistry;
import com.uestcfir.auth.SessionUser;
import com.uestcfir.mapper.IotDeviceMapper;
import com.uestcfir.mapper.ReservationMapper;
import com.uestcfir.pojo.entity.IotDevice;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IotControlAuthorizationServiceTest {
    @Mock
    private IotDeviceMapper iotDeviceMapper;

    @Mock
    private ReservationMapper reservationMapper;

    @InjectMocks
    private IotControlAuthorizationService service;

    @AfterEach
    void clearUserContext() {
        CurrentUserContext.clear();
    }

    @Test
    void meetingRoomAdminCanControlAnyKnownDevice() {
        IotDevice device = device("raspi-02", 15);
        when(iotDeviceMapper.findByDeviceId("raspi-02")).thenReturn(device);
        CurrentUserContext.set(user(10, 0, Set.of(RolePermissionRegistry.IOT_CONTROL)));

        IotDevice result = assertDoesNotThrow(() -> service.requireCanControl("raspi-02"));

        assertEquals(15, result.getRoomId());
        verify(reservationMapper, never()).existsActiveReservation(any(), any(), any(), any());
    }

    @Test
    void normalUserCanControlDuringOwnApprovedReservation() {
        IotDevice device = device("raspi-01", 14);
        when(iotDeviceMapper.findByDeviceId("raspi-01")).thenReturn(device);
        when(reservationMapper.existsActiveReservation(eq(18), eq(14), any(), any())).thenReturn(true);
        CurrentUserContext.set(user(18, 1, Set.of(RolePermissionRegistry.IOT_READ)));

        assertDoesNotThrow(() -> service.requireCanControl("raspi-01"));
        verify(reservationMapper).existsActiveReservation(eq(18), eq(14), any(), any());
    }

    @Test
    void rbacSuperAdminRoleCanControlRegardlessOfLegacyUserType() {
        IotDevice device = device("raspi-03", 32);
        when(iotDeviceMapper.findByDeviceId("raspi-03")).thenReturn(device);
        CurrentUserContext.set(SessionUser.builder()
                .userId(18)
                .userType(1)
                .role("super_admin")
                .permissions(Set.of(RolePermissionRegistry.IOT_CONTROL))
                .build());

        assertDoesNotThrow(() -> service.requireCanControl("raspi-03"));
        verify(reservationMapper, never()).existsActiveReservation(any(), any(), any(), any());
    }

    @Test
    void normalUserIsDeniedWithoutActiveReservation() {
        IotDevice device = device("raspi-03", 32);
        when(iotDeviceMapper.findByDeviceId("raspi-03")).thenReturn(device);
        when(reservationMapper.existsActiveReservation(eq(18), eq(32), any(), any())).thenReturn(false);
        CurrentUserContext.set(user(18, 1, Set.of(RolePermissionRegistry.IOT_READ)));

        assertThrows(RuntimeException.class, () -> service.requireCanControl("raspi-03"));
    }

    @Test
    void unknownDeviceIsDeniedBeforeReservationLookup() {
        when(iotDeviceMapper.findByDeviceId("missing")).thenReturn(null);
        CurrentUserContext.set(user(18, 1, Set.of(RolePermissionRegistry.IOT_READ)));

        assertThrows(RuntimeException.class, () -> service.requireCanControl("missing"));
        verify(reservationMapper, never()).existsActiveReservation(any(), any(), any(), any());
    }

    private IotDevice device(String deviceId, Integer roomId) {
        IotDevice device = new IotDevice();
        device.setDeviceId(deviceId);
        device.setRoomId(roomId);
        return device;
    }

    private SessionUser user(Integer userId, Integer userType, Set<String> permissions) {
        return SessionUser.builder()
                .userId(userId)
                .userType(userType)
                .permissions(permissions)
                .build();
    }
}
