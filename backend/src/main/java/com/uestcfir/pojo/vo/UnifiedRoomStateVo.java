package com.uestcfir.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.uestcfir.pojo.entity.IotRoomStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Stable room-state contract shared by the Web, backend, Raspberry Pi and 3D clients.
 */
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class UnifiedRoomStateVo {
    private LocalDateTime timestamp;

    @JsonProperty("room_id")
    private String roomId;

    @JsonProperty("room_db_id")
    private Integer roomDbId;

    @JsonProperty("device_id")
    private String deviceId;

    private Boolean occupancy;

    @JsonProperty("people_count")
    private Integer peopleCount;

    private BigDecimal temperature;
    private BigDecimal humidity;

    @JsonProperty("door_status")
    private String doorStatus;

    private Devices devices;

    @JsonProperty("sensor_status")
    private String sensorStatus;

    private String source;
    private String quality;

    @JsonProperty("quality_issues")
    private List<String> qualityIssues;

    public static UnifiedRoomStateVo fromRoomStatus(IotRoomStatus roomStatus) {
        if (roomStatus == null) {
            return null;
        }

        UnifiedRoomStateVo state = new UnifiedRoomStateVo();
        state.setTimestamp(roomStatus.getLastReportedAt());
        state.setRoomDbId(roomStatus.getRoomId());
        state.setRoomId(resolveRoomCode(roomStatus.getRoomCode(), roomStatus.getRoomId()));
        state.setDeviceId(roomStatus.getDeviceId());
        state.setOccupancy(firstNonNull(
                roomStatus.getPresence(),
                roomStatus.getMotionDetected(),
                roomStatus.getPersonNear()
        ));
        state.setPeopleCount(roomStatus.getPeopleCount());
        state.setTemperature(roomStatus.getTemperature());
        state.setHumidity(roomStatus.getHumidity());
        state.setDoorStatus(resolveDoorStatus(roomStatus.getDoorState(), roomStatus.getDoorOpen()));
        state.setDevices(Devices.fromRoomStatus(roomStatus));
        state.setSensorStatus(normalizeSensorStatus(roomStatus.getSensorStatus()));
        state.setSource("sensor");

        List<String> issues = validateState(state, roomStatus.getErrorMessage());
        state.setQualityIssues(issues);
        state.setQuality(issues.isEmpty() ? "normal" : "abnormal");
        return state;
    }

    private static List<String> validateState(UnifiedRoomStateVo state, String errorMessage) {
        List<String> issues = new ArrayList<>();
        if (state.getTimestamp() == null) {
            issues.add("missing_timestamp");
        }
        if (isBlank(state.getRoomId())) {
            issues.add("missing_room_id");
        }
        if (state.getRoomDbId() == null) {
            issues.add("missing_room_db_id");
        }
        if (isBlank(state.getDeviceId())) {
            issues.add("missing_device_id");
        }
        if (state.getOccupancy() == null) {
            issues.add("missing_occupancy");
        }
        if (state.getTemperature() == null) {
            issues.add("missing_temperature");
        } else if (outside(state.getTemperature(), 0, 50)) {
            issues.add("temperature_out_of_range");
        }
        if (state.getHumidity() == null) {
            issues.add("missing_humidity");
        } else if (outside(state.getHumidity(), 0, 100)) {
            issues.add("humidity_out_of_range");
        }
        if (isBlank(state.getDoorStatus())) {
            issues.add("missing_door_status");
        } else if (!List.of("open", "closed", "opening", "closing").contains(state.getDoorStatus())) {
            issues.add("invalid_door_status");
        }
        if (!"ok".equals(state.getSensorStatus())) {
            issues.add("sensor_status_" + state.getSensorStatus());
        }
        if (!isBlank(errorMessage)) {
            issues.add("device_error");
        }
        return issues;
    }

    private static boolean outside(BigDecimal value, int minimum, int maximum) {
        return value.compareTo(BigDecimal.valueOf(minimum)) < 0
                || value.compareTo(BigDecimal.valueOf(maximum)) > 0;
    }

    private static String resolveRoomCode(String roomCode, Integer roomId) {
        if (!isBlank(roomCode)) {
            return roomCode.trim().toLowerCase();
        }
        return roomId == null ? null : String.format("meeting_room_%02d", roomId);
    }

    private static String resolveDoorStatus(String doorState, Boolean doorOpen) {
        if (!isBlank(doorState)) {
            String normalized = doorState.trim().toLowerCase();
            if ("opened".equals(normalized)) {
                return "open";
            }
            if ("close".equals(normalized)) {
                return "closed";
            }
            return normalized;
        }
        if (doorOpen == null) {
            return null;
        }
        return doorOpen ? "open" : "closed";
    }

    private static String normalizeSensorStatus(String sensorStatus) {
        return isBlank(sensorStatus) ? "unknown" : sensorStatus.trim().toLowerCase();
    }

    private static Boolean firstNonNull(Boolean... values) {
        for (Boolean value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Data
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class Devices {
        @JsonProperty("light_on")
        private Boolean lightOn;

        @JsonProperty("projector_on")
        private Boolean projectorOn;

        private Boolean cooling;
        private Boolean heating;
        private Boolean humidifier;
        private Boolean dehumidifier;

        @JsonProperty("curtain_open")
        private Boolean curtainOpen;

        @JsonProperty("buzzer_on")
        private Boolean buzzerOn;

        @JsonProperty("auto_mode")
        private Boolean autoMode;

        private Boolean alarm;

        private static Devices fromRoomStatus(IotRoomStatus roomStatus) {
            Devices devices = new Devices();
            devices.setLightOn(roomStatus.getLightOn());
            devices.setProjectorOn(roomStatus.getProjector());
            devices.setCooling(roomStatus.getCooling());
            devices.setHeating(roomStatus.getHeating());
            devices.setHumidifier(roomStatus.getHumidifier());
            devices.setDehumidifier(roomStatus.getDehumidifier());
            devices.setCurtainOpen(roomStatus.getCurtainOpen());
            devices.setBuzzerOn(roomStatus.getBuzzerOn());
            devices.setAutoMode(roomStatus.getAutoMode());
            devices.setAlarm(roomStatus.getAlarm());
            return devices;
        }
    }
}
