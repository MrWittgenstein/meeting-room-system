package com.uestcfir.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.uestcfir.pojo.entity.IotRoomStatus;
import com.uestcfir.pojo.entity.IotSensorRecord;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IotTelemetryVo {
    @JsonProperty("device_id")
    private String deviceId;

    @JsonProperty("room_id")
    private String roomId;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    private Environment environment;
    private Presence presence;

    @JsonProperty("access_control")
    private AccessControl accessControl;

    private Devices devices;
    private TelemetrySystem system;

    public static IotTelemetryVo fromEntity(IotSensorRecord record) {
        if (record == null) {
            return null;
        }
        return buildPayload(
                record.getDeviceId(),
                record.getRoomCode(),
                record.getRoomId(),
                record.getEventType(),
                record.getReportedAt(),
                record.getTemperature(),
                record.getHumidity(),
                record.getTemperatureStatus(),
                record.getHumidityStatus(),
                record.getCo2Ppm(),
                record.getLight(),
                record.getLightRaw(),
                record.getLightStatus(),
                record.getLightLux(),
                record.getSmoke(),
                record.getSmokeRaw(),
                record.getSmokeLevel(),
                record.getNoiseDb(),
                record.getDistanceCm(),
                record.getPersonNear(),
                record.getPresence(),
                record.getLastMotionTime(),
                record.getPeopleCount(),
                record.getMotionDetected(),
                record.getFaceDetected(),
                record.getFaceCount(),
                record.getAuthorized(),
                record.getAccessResult(),
                record.getDoorState(),
                record.getServoAngle(),
                record.getDoorOpen(),
                record.getAlarm(),
                record.getCooling(),
                record.getHeating(),
                record.getHumidifier(),
                record.getDehumidifier(),
                record.getProjector(),
                record.getLightOn(),
                record.getCurtainOpen(),
                record.getBuzzerOn(),
                record.getAutoMode(),
                record.getSensorStatus(),
                record.getErrorMessage()
        );
    }

    public static IotTelemetryVo fromRoomStatus(IotRoomStatus roomStatus) {
        if (roomStatus == null) {
            return null;
        }
        return buildPayload(
                roomStatus.getDeviceId(),
                roomStatus.getRoomCode(),
                roomStatus.getRoomId(),
                roomStatus.getEventType(),
                roomStatus.getLastReportedAt(),
                roomStatus.getTemperature(),
                roomStatus.getHumidity(),
                roomStatus.getTemperatureStatus(),
                roomStatus.getHumidityStatus(),
                roomStatus.getCo2Ppm(),
                roomStatus.getLight(),
                roomStatus.getLightRaw(),
                roomStatus.getLightStatus(),
                roomStatus.getLightLux(),
                roomStatus.getSmoke(),
                roomStatus.getSmokeRaw(),
                roomStatus.getSmokeLevel(),
                roomStatus.getNoiseDb(),
                roomStatus.getDistanceCm(),
                roomStatus.getPersonNear(),
                roomStatus.getPresence(),
                roomStatus.getLastMotionTime(),
                roomStatus.getPeopleCount(),
                roomStatus.getMotionDetected(),
                roomStatus.getFaceDetected(),
                roomStatus.getFaceCount(),
                roomStatus.getAuthorized(),
                roomStatus.getAccessResult(),
                roomStatus.getDoorState(),
                roomStatus.getServoAngle(),
                roomStatus.getDoorOpen(),
                roomStatus.getAlarm(),
                roomStatus.getCooling(),
                roomStatus.getHeating(),
                roomStatus.getHumidifier(),
                roomStatus.getDehumidifier(),
                roomStatus.getProjector(),
                roomStatus.getLightOn(),
                roomStatus.getCurtainOpen(),
                roomStatus.getBuzzerOn(),
                roomStatus.getAutoMode(),
                roomStatus.getSensorStatus(),
                roomStatus.getErrorMessage()
        );
    }

    private static IotTelemetryVo buildPayload(String deviceId,
                                               String roomCode,
                                               Integer roomId,
                                               String eventType,
                                               LocalDateTime timestamp,
                                               BigDecimal temperature,
                                               BigDecimal humidity,
                                               String temperatureStatus,
                                               String humidityStatus,
                                               BigDecimal co2Ppm,
                                               Integer light,
                                               Integer lightRaw,
                                               String lightStatus,
                                               BigDecimal lightLux,
                                               BigDecimal smoke,
                                               Integer smokeRaw,
                                               String smokeLevel,
                                               BigDecimal noiseDb,
                                               BigDecimal distanceCm,
                                               Boolean personNear,
                                               Boolean presenceValue,
                                               LocalDateTime lastMotionTime,
                                               Integer peopleCount,
                                               Boolean motionDetected,
                                               Boolean faceDetected,
                                               Integer faceCount,
                                               Boolean authorized,
                                               String accessResult,
                                               String doorState,
                                               Integer servoAngle,
                                               Boolean doorOpen,
                                               Boolean alarm,
                                               Boolean cooling,
                                               Boolean heating,
                                               Boolean humidifier,
                                               Boolean dehumidifier,
                                               Boolean projector,
                                               Boolean lightOn,
                                               Boolean curtainOpen,
                                               Boolean buzzerOn,
                                               Boolean autoMode,
                                               String sensorStatus,
                                               String errorMessage) {
        IotTelemetryVo vo = new IotTelemetryVo();
        vo.setDeviceId(deviceId);
        vo.setRoomId(defaultRoomCode(roomCode, roomId));
        vo.setEventType(eventType);
        vo.setTimestamp(timestamp);

        Environment environment = new Environment();
        environment.setTemperature(temperature);
        environment.setHumidity(humidity);
        environment.setTemperatureStatus(defaultIfBlank(temperatureStatus, deriveTemperatureStatus(temperature)));
        environment.setHumidityStatus(defaultIfBlank(humidityStatus, deriveHumidityStatus(humidity)));
        environment.setCo2Ppm(co2Ppm);
        environment.setLight(light);
        environment.setLightRaw(lightRaw);
        environment.setLightStatus(defaultIfBlank(lightStatus, deriveLightStatus(light)));
        environment.setLightLux(lightLux);
        environment.setSmoke(smoke);
        environment.setSmokeRaw(smokeRaw);
        environment.setSmokeLevel(defaultIfBlank(smokeLevel, deriveSmokeLevel(smoke)));
        environment.setNoiseDb(noiseDb);
        vo.setEnvironment(environment);

        Presence presence = new Presence();
        Boolean normalizedPresence = firstNonNull(presenceValue, motionDetected, personNear);
        Boolean normalizedPersonNear = personNear != null ? personNear : derivePersonNear(distanceCm);
        presence.setPir(defaultBoolean(normalizedPresence));
        presence.setPresence(defaultBoolean(normalizedPresence));
        presence.setDistance(distanceCm);
        presence.setPersonNear(defaultBoolean(normalizedPersonNear));
        presence.setLastMotionTime(lastMotionTime);
        presence.setPeopleCount(peopleCount);
        vo.setPresence(presence);

        AccessControl accessControl = new AccessControl();
        accessControl.setFaceDetected(defaultBoolean(faceDetected));
        accessControl.setFaceCount(faceCount == null ? 0 : faceCount);
        accessControl.setDoorState(defaultIfBlank(doorState, deriveDoorState(doorOpen)));
        accessControl.setAuthorized(defaultBoolean(authorized));
        accessControl.setAccessResult(accessResult);
        accessControl.setServoAngle(servoAngle);
        vo.setAccessControl(accessControl);

        Devices devices = new Devices();
        devices.setCooling(defaultBoolean(cooling));
        devices.setHeating(defaultBoolean(heating));
        devices.setHumidifier(defaultBoolean(humidifier));
        devices.setDehumidifier(defaultBoolean(dehumidifier));
        devices.setProjector(defaultBoolean(projector));
        devices.setLightOn(defaultBoolean(lightOn));
        devices.setCurtainOpen(defaultBoolean(curtainOpen));
        devices.setBuzzerOn(defaultBoolean(buzzerOn));
        devices.setAutoMode(defaultBoolean(autoMode));
        devices.setAlarm(defaultBoolean(alarm));
        vo.setDevices(devices);

        TelemetrySystem system = new TelemetrySystem();
        system.setSensorStatus(defaultIfBlank(sensorStatus, errorMessage == null ? "ok" : "error"));
        system.setError(errorMessage);
        vo.setSystem(system);

        return vo;
    }

    private static String defaultRoomCode(String roomCode, Integer roomId) {
        if (roomCode != null && !roomCode.isBlank()) {
            return roomCode;
        }
        if (roomId == null) {
            return null;
        }
        return String.format("meeting_room_%02d", roomId);
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static Boolean defaultBoolean(Boolean value) {
        return value != null && value;
    }

    private static Boolean firstNonNull(Boolean... values) {
        for (Boolean value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static Boolean derivePersonNear(BigDecimal distanceCm) {
        if (distanceCm == null) {
            return null;
        }
        return distanceCm.compareTo(BigDecimal.valueOf(100)) < 0;
    }

    private static String deriveDoorState(Boolean doorOpen) {
        if (doorOpen == null) {
            return null;
        }
        return doorOpen ? "open" : "closed";
    }

    private static String deriveTemperatureStatus(BigDecimal temperature) {
        if (temperature == null) {
            return null;
        }
        if (temperature.compareTo(BigDecimal.valueOf(28)) > 0) {
            return "hot";
        }
        if (temperature.compareTo(BigDecimal.valueOf(20)) < 0) {
            return "cold";
        }
        return "comfortable";
    }

    private static String deriveHumidityStatus(BigDecimal humidity) {
        if (humidity == null) {
            return null;
        }
        if (humidity.compareTo(BigDecimal.valueOf(65)) > 0) {
            return "too_humid";
        }
        if (humidity.compareTo(BigDecimal.valueOf(40)) < 0) {
            return "too_dry";
        }
        return "normal";
    }

    private static String deriveLightStatus(Integer light) {
        if (light == null) {
            return null;
        }
        if (light < 30) {
            return "dark";
        }
        if (light > 70) {
            return "bright";
        }
        return "normal";
    }

    private static String deriveSmokeLevel(BigDecimal smoke) {
        if (smoke == null) {
            return null;
        }
        if (smoke.compareTo(BigDecimal.valueOf(900)) > 0) {
            return "critical";
        }
        if (smoke.compareTo(BigDecimal.valueOf(600)) > 0) {
            return "danger";
        }
        if (smoke.compareTo(BigDecimal.valueOf(300)) >= 0) {
            return "warning";
        }
        return "normal";
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Environment {
        private BigDecimal temperature;
        private BigDecimal humidity;

        @JsonProperty("temperature_status")
        private String temperatureStatus;

        @JsonProperty("humidity_status")
        private String humidityStatus;

        @JsonProperty("co2_ppm")
        private BigDecimal co2Ppm;

        private Integer light;

        @JsonProperty("light_raw")
        private Integer lightRaw;

        @JsonProperty("light_status")
        private String lightStatus;

        @JsonProperty("light_lux")
        private BigDecimal lightLux;

        private BigDecimal smoke;

        @JsonProperty("smoke_raw")
        private Integer smokeRaw;

        @JsonProperty("smoke_level")
        private String smokeLevel;

        @JsonProperty("noise_db")
        private BigDecimal noiseDb;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Presence {
        private Boolean pir;
        private Boolean presence;
        private BigDecimal distance;

        @JsonProperty("person_near")
        private Boolean personNear;

        @JsonProperty("last_motion_time")
        private LocalDateTime lastMotionTime;

        @JsonProperty("people_count")
        private Integer peopleCount;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AccessControl {
        @JsonProperty("face_detected")
        private Boolean faceDetected;

        @JsonProperty("face_count")
        private Integer faceCount;

        @JsonProperty("door_state")
        private String doorState;

        private Boolean authorized;

        @JsonProperty("access_result")
        private String accessResult;

        @JsonProperty("servo_angle")
        private Integer servoAngle;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Devices {
        private Boolean projector;
        private Boolean cooling;
        private Boolean heating;
        private Boolean humidifier;
        private Boolean dehumidifier;

        @JsonProperty("light_on")
        private Boolean lightOn;

        @JsonProperty("curtain_open")
        private Boolean curtainOpen;

        @JsonProperty("buzzer_on")
        private Boolean buzzerOn;

        @JsonProperty("auto_mode")
        private Boolean autoMode;

        private Boolean alarm;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TelemetrySystem {
        @JsonProperty("sensor_status")
        private String sensorStatus;

        private String error;
    }
}
