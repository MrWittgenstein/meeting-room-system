package com.uestcfir.pojo.vo;

import com.uestcfir.pojo.entity.IotRoomStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnifiedRoomStateVoTest {

    @Test
    void mapsValidRoomStatusToUnifiedContract() {
        IotRoomStatus roomStatus = baseRoomStatus();
        roomStatus.setPresence(true);
        roomStatus.setTemperature(new BigDecimal("24.6"));
        roomStatus.setHumidity(new BigDecimal("48.0"));
        roomStatus.setDoorState("closed");
        roomStatus.setSensorStatus("ok");
        roomStatus.setLightOn(true);
        roomStatus.setProjector(false);
        roomStatus.setAutoMode(true);

        UnifiedRoomStateVo state = UnifiedRoomStateVo.fromRoomStatus(roomStatus);

        assertEquals("meeting_room_14", state.getRoomId());
        assertEquals(14, state.getRoomDbId());
        assertTrue(state.getOccupancy());
        assertEquals("closed", state.getDoorStatus());
        assertEquals("sensor", state.getSource());
        assertEquals("normal", state.getQuality());
        assertTrue(state.getQualityIssues().isEmpty());
        assertTrue(state.getDevices().getLightOn());
        assertFalse(state.getDevices().getProjectorOn());
    }

    @Test
    void marksMissingAndInvalidValuesAsAbnormal() {
        IotRoomStatus roomStatus = baseRoomStatus();
        roomStatus.setPresence(null);
        roomStatus.setMotionDetected(null);
        roomStatus.setPersonNear(null);
        roomStatus.setTemperature(new BigDecimal("85"));
        roomStatus.setHumidity(null);
        roomStatus.setDoorState("ajar");
        roomStatus.setSensorStatus("error");
        roomStatus.setErrorMessage("sensor read failed");

        UnifiedRoomStateVo state = UnifiedRoomStateVo.fromRoomStatus(roomStatus);

        assertEquals("abnormal", state.getQuality());
        assertTrue(state.getQualityIssues().contains("missing_occupancy"));
        assertTrue(state.getQualityIssues().contains("temperature_out_of_range"));
        assertFalse(state.getQualityIssues().contains("missing_temperature"));
        assertTrue(state.getQualityIssues().contains("missing_humidity"));
        assertTrue(state.getQualityIssues().contains("invalid_door_status"));
        assertTrue(state.getQualityIssues().contains("sensor_status_error"));
        assertTrue(state.getQualityIssues().contains("device_error"));
    }

    @Test
    void doesNotReportMissingTemperatureAsOutOfRange() {
        IotRoomStatus roomStatus = baseRoomStatus();
        roomStatus.setPresence(true);
        roomStatus.setTemperature(null);
        roomStatus.setHumidity(new BigDecimal("48.0"));
        roomStatus.setDoorState("closed");
        roomStatus.setSensorStatus("ok");

        UnifiedRoomStateVo state = UnifiedRoomStateVo.fromRoomStatus(roomStatus);

        assertTrue(state.getQualityIssues().contains("missing_temperature"));
        assertFalse(state.getQualityIssues().contains("temperature_out_of_range"));
    }

    private IotRoomStatus baseRoomStatus() {
        IotRoomStatus roomStatus = new IotRoomStatus();
        roomStatus.setRoomCode("meeting_room_14");
        roomStatus.setRoomId(14);
        roomStatus.setDeviceId("raspi-01");
        roomStatus.setLastReportedAt(LocalDateTime.of(2026, 9, 20, 10, 30));
        roomStatus.setPeopleCount(3);
        return roomStatus;
    }
}
