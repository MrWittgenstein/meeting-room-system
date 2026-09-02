package com.uestcfir.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uestcfir.exception.BusinessException;
import com.uestcfir.mapper.IotDeviceMapper;
import com.uestcfir.mapper.IotRoomStatusMapper;
import com.uestcfir.mapper.IotSensorRecordMapper;
import com.uestcfir.mapper.MeetingroomMapper;
import com.uestcfir.pojo.dto.IotTelemetryRequest;
import com.uestcfir.pojo.entity.IotDevice;
import com.uestcfir.pojo.entity.IotRoomStatus;
import com.uestcfir.pojo.entity.IotSensorRecord;
import com.uestcfir.pojo.entity.Meetingroom;
import com.uestcfir.pojo.vo.IotTelemetryVo;
import com.uestcfir.service.IotTelemetryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class IotTelemetryServiceImpl implements IotTelemetryService {
    private static final String DEFAULT_DEVICE_TYPE = "raspberrypi";
    private static final String DEFAULT_PROTOCOL = "websocket";
    private static final String DEFAULT_ONLINE_STATUS = "online";
    private static final Pattern TRAILING_NUMBER_PATTERN = Pattern.compile("(\\d+)$");

    @Autowired
    private IotDeviceMapper iotDeviceMapper;

    @Autowired
    private IotRoomStatusMapper iotRoomStatusMapper;

    @Autowired
    private IotSensorRecordMapper iotSensorRecordMapper;

    @Autowired
    private MeetingroomMapper meetingroomMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional
    public IotSensorRecord saveTelemetry(IotTelemetryRequest request, String rawPayloadJson) {
        validateRequest(request);
        String deviceId = request.getDeviceId().trim();
        ResolvedRoom resolvedRoom = resolveRoom(request, deviceId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reportedAt = request.getReportedAt() == null ? now : request.getReportedAt();
        String roomCode = resolveRoomCode(request.getRoomCode(), resolvedRoom.meetingroom());

        request.setDeviceId(deviceId);
        request.setRoomId(resolvedRoom.meetingroom().getRoomId());
        request.setRoomCode(roomCode);
        request.setReportedAt(reportedAt);
        request.setEventType(resolveEventType(request.getEventType()));
        normalizeRequest(request);

        upsertDevice(deviceId, roomCode, request.getRoomId(), reportedAt, now);

        IotSensorRecord record = buildRecord(request, now);
        record.setPayloadJson(buildNormalizedPayloadJson(record, rawPayloadJson));

        iotSensorRecordMapper.insert(record);
        record.setPayloadJson(buildNormalizedPayloadJson(record, rawPayloadJson));
        upsertRoomStatus(record, now);

        log.info("Saved iot telemetry. deviceId={}, roomId={}, roomCode={}, recordId={}",
                record.getDeviceId(), record.getRoomId(), record.getRoomCode(), record.getId());
        return record;
    }

    @Override
    public IotSensorRecord getLatestByRoomId(Integer roomId) {
        validateRoomId(roomId);
        IotSensorRecord record = iotSensorRecordMapper.findLatestByRoomId(roomId);
        if (record == null) {
            throw new BusinessException("No IoT telemetry found for roomId=" + roomId);
        }
        return record;
    }

    @Override
    public List<IotSensorRecord> getRecentByRoomId(Integer roomId, Integer limit) {
        validateRoomId(roomId);
        int safeLimit = limit == null ? 20 : Math.max(1, Math.min(limit, 200));
        return iotSensorRecordMapper.findRecentByRoomId(roomId, safeLimit);
    }

    @Override
    public IotSensorRecord getLatestByDeviceId(String deviceId) {
        if (StringUtils.isBlank(deviceId)) {
            throw new BusinessException("deviceId can not be empty");
        }
        IotSensorRecord record = iotSensorRecordMapper.findLatestByDeviceId(deviceId.trim());
        if (record == null) {
            throw new BusinessException("No IoT telemetry found for deviceId=" + deviceId);
        }
        return record;
    }

    @Override
    public IotRoomStatus getRoomStatus(Integer roomId) {
        validateRoomId(roomId);
        IotRoomStatus roomStatus = iotRoomStatusMapper.findByRoomId(roomId);
        if (roomStatus == null) {
            throw new BusinessException("No IoT room status found for roomId=" + roomId);
        }
        return roomStatus;
    }

    @Override
    public IotDevice getDevice(String deviceId) {
        if (StringUtils.isBlank(deviceId)) {
            throw new BusinessException("deviceId can not be empty");
        }
        IotDevice device = iotDeviceMapper.findByDeviceId(deviceId.trim());
        if (device == null) {
            throw new BusinessException("IoT device does not exist, deviceId=" + deviceId);
        }
        return device;
    }

    private void validateRequest(IotTelemetryRequest request) {
        if (request == null) {
            throw new BusinessException("iot telemetry request can not be empty");
        }
        if (StringUtils.isBlank(request.getDeviceId())) {
            throw new BusinessException("deviceId or device_id is required");
        }
    }

    private void validateRoomId(Integer roomId) {
        if (roomId == null) {
            throw new BusinessException("roomId is required");
        }
        if (meetingroomMapper.getMeetingroomById(roomId) == null) {
            throw new BusinessException("meeting room does not exist, roomId=" + roomId);
        }
    }

    private String resolveEventType(String eventType) {
        return StringUtils.isBlank(eventType) ? "telemetry" : eventType.trim().toLowerCase();
    }

    private ResolvedRoom resolveRoom(IotTelemetryRequest request, String deviceId) {
        if (request.getRoomId() != null) {
            Meetingroom meetingroom = meetingroomMapper.getMeetingroomById(request.getRoomId());
            if (meetingroom == null) {
                throw new BusinessException("meeting room does not exist, roomId=" + request.getRoomId());
            }
            return new ResolvedRoom(meetingroom);
        }

        if (StringUtils.isNotBlank(request.getRoomCode())) {
            Meetingroom meetingroom = findMeetingroomByRoomCode(request.getRoomCode().trim());
            if (meetingroom != null) {
                return new ResolvedRoom(meetingroom);
            }
        }

        IotDevice device = iotDeviceMapper.findByDeviceId(deviceId);
        if (device != null && device.getRoomId() != null) {
            Meetingroom meetingroom = meetingroomMapper.getMeetingroomById(device.getRoomId());
            if (meetingroom != null) {
                return new ResolvedRoom(meetingroom);
            }
        }

        throw new BusinessException("roomId or room_id is required for the first telemetry report");
    }

    private Meetingroom findMeetingroomByRoomCode(String roomCode) {
        Integer trailingNumber = extractTrailingNumber(roomCode);
        if (trailingNumber == null) {
            return null;
        }

        Meetingroom byRoomNumber = meetingroomMapper.getMeetingroomByRoomNumber(trailingNumber);
        if (byRoomNumber != null) {
            return byRoomNumber;
        }
        return meetingroomMapper.getMeetingroomById(trailingNumber);
    }

    private Integer extractTrailingNumber(String roomCode) {
        if (StringUtils.isBlank(roomCode)) {
            return null;
        }
        Matcher matcher = TRAILING_NUMBER_PATTERN.matcher(roomCode);
        if (!matcher.find()) {
            return null;
        }
        return Integer.valueOf(matcher.group(1));
    }

    private String resolveRoomCode(String roomCode, Meetingroom meetingroom) {
        if (StringUtils.isNotBlank(roomCode)) {
            return roomCode.trim();
        }
        Integer roomNumber = meetingroom.getRoomNumber() == null ? meetingroom.getRoomId() : meetingroom.getRoomNumber();
        return String.format("meeting_room_%02d", roomNumber);
    }

    private void normalizeRequest(IotTelemetryRequest request) {
        request.setTemperatureStatus(defaultIfBlank(request.getTemperatureStatus(), deriveTemperatureStatus(request.getTemperature())));
        request.setHumidityStatus(defaultIfBlank(request.getHumidityStatus(), deriveHumidityStatus(request.getHumidity())));
        request.setLightStatus(defaultIfBlank(request.getLightStatus(), deriveLightStatus(request.getLight())));
        request.setSmokeLevel(defaultIfBlank(request.getSmokeLevel(), deriveSmokeLevel(request.getSmoke())));

        if (request.getPersonNear() == null && request.getDistanceCm() != null) {
            request.setPersonNear(request.getDistanceCm().compareTo(BigDecimal.valueOf(100)) < 0);
        }
        if (request.getPresence() == null) {
            request.setPresence(firstNonNull(request.getMotionDetected(), request.getPersonNear()));
        }
        if (request.getMotionDetected() == null) {
            request.setMotionDetected(request.getPresence());
        }
        if (request.getDoorState() == null && request.getDoorOpen() != null) {
            request.setDoorState(request.getDoorOpen() ? "open" : "closed");
        }
        if (request.getDoorOpen() == null && StringUtils.isNotBlank(request.getDoorState())) {
            request.setDoorOpen(isDoorOpenState(request.getDoorState()));
        }
        if (request.getAlarm() == null) {
            request.setAlarm(Boolean.TRUE.equals(request.getBuzzerOn()) || isAlertSmokeLevel(request.getSmokeLevel()));
        }
        request.setSensorStatus(defaultIfBlank(request.getSensorStatus(),
                StringUtils.isBlank(request.getErrorMessage()) ? "ok" : "error"));
    }

    private IotSensorRecord buildRecord(IotTelemetryRequest request, LocalDateTime now) {
        IotSensorRecord record = new IotSensorRecord();
        record.setDeviceId(request.getDeviceId());
        record.setRoomCode(request.getRoomCode());
        record.setRoomId(request.getRoomId());
        record.setEventType(request.getEventType());
        record.setTemperature(request.getTemperature());
        record.setHumidity(request.getHumidity());
        record.setTemperatureStatus(request.getTemperatureStatus());
        record.setHumidityStatus(request.getHumidityStatus());
        record.setCo2Ppm(request.getCo2Ppm());
        record.setLight(request.getLight());
        record.setLightRaw(request.getLightRaw());
        record.setLightStatus(request.getLightStatus());
        record.setLightLux(request.getLightLux());
        record.setSmoke(request.getSmoke());
        record.setSmokeRaw(request.getSmokeRaw());
        record.setSmokeLevel(request.getSmokeLevel());
        record.setNoiseDb(request.getNoiseDb());
        record.setDistanceCm(request.getDistanceCm());
        record.setPersonNear(request.getPersonNear());
        record.setPresence(request.getPresence());
        record.setLastMotionTime(request.getLastMotionTime());
        record.setPeopleCount(request.getPeopleCount());
        record.setMotionDetected(request.getMotionDetected());
        record.setFaceDetected(request.getFaceDetected());
        record.setFaceCount(request.getFaceCount());
        record.setAuthorized(request.getAuthorized());
        record.setAccessResult(request.getAccessResult());
        record.setDoorState(request.getDoorState());
        record.setServoAngle(request.getServoAngle());
        record.setDoorOpen(request.getDoorOpen());
        record.setAlarm(request.getAlarm());
        record.setCooling(request.getCooling());
        record.setHeating(request.getHeating());
        record.setHumidifier(request.getHumidifier());
        record.setDehumidifier(request.getDehumidifier());
        record.setProjector(request.getProjector());
        record.setLightOn(request.getLightOn());
        record.setCurtainOpen(request.getCurtainOpen());
        record.setBuzzerOn(request.getBuzzerOn());
        record.setAutoMode(request.getAutoMode());
        record.setSensorStatus(request.getSensorStatus());
        record.setErrorMessage(request.getErrorMessage());
        record.setReportedAt(request.getReportedAt());
        record.setCreateTime(now);
        record.setUpdateTime(now);
        return record;
    }

    private void upsertDevice(String deviceId, String roomCode, Integer roomId, LocalDateTime reportedAt, LocalDateTime now) {
        IotDevice device = iotDeviceMapper.findByDeviceId(deviceId);
        IotDevice current = new IotDevice();
        current.setDeviceId(deviceId);
        current.setRoomCode(roomCode);
        current.setRoomId(roomId);
        current.setDeviceName(device == null ? deviceId : device.getDeviceName());
        current.setDeviceType(device == null ? DEFAULT_DEVICE_TYPE : defaultIfBlank(device.getDeviceType(), DEFAULT_DEVICE_TYPE));
        current.setProtocol(device == null ? DEFAULT_PROTOCOL : defaultIfBlank(device.getProtocol(), DEFAULT_PROTOCOL));
        current.setOnlineStatus(DEFAULT_ONLINE_STATUS);
        current.setLastSeenAt(now);
        current.setLastReportedAt(reportedAt);
        current.setCreateTime(device == null ? now : device.getCreateTime());
        current.setUpdateTime(now);
        iotDeviceMapper.upsert(current);
    }

    private void upsertRoomStatus(IotSensorRecord record, LocalDateTime now) {
        IotRoomStatus roomStatus = new IotRoomStatus();
        roomStatus.setRoomCode(record.getRoomCode());
        roomStatus.setRoomId(record.getRoomId());
        roomStatus.setDeviceId(record.getDeviceId());
        roomStatus.setLatestRecordId(record.getId());
        roomStatus.setEventType(record.getEventType());
        roomStatus.setTemperature(record.getTemperature());
        roomStatus.setHumidity(record.getHumidity());
        roomStatus.setTemperatureStatus(record.getTemperatureStatus());
        roomStatus.setHumidityStatus(record.getHumidityStatus());
        roomStatus.setCo2Ppm(record.getCo2Ppm());
        roomStatus.setLight(record.getLight());
        roomStatus.setLightRaw(record.getLightRaw());
        roomStatus.setLightStatus(record.getLightStatus());
        roomStatus.setLightLux(record.getLightLux());
        roomStatus.setSmoke(record.getSmoke());
        roomStatus.setSmokeRaw(record.getSmokeRaw());
        roomStatus.setSmokeLevel(record.getSmokeLevel());
        roomStatus.setNoiseDb(record.getNoiseDb());
        roomStatus.setDistanceCm(record.getDistanceCm());
        roomStatus.setPersonNear(record.getPersonNear());
        roomStatus.setPresence(record.getPresence());
        roomStatus.setLastMotionTime(record.getLastMotionTime());
        roomStatus.setPeopleCount(record.getPeopleCount());
        roomStatus.setMotionDetected(record.getMotionDetected());
        roomStatus.setFaceDetected(record.getFaceDetected());
        roomStatus.setFaceCount(record.getFaceCount());
        roomStatus.setAuthorized(record.getAuthorized());
        roomStatus.setAccessResult(record.getAccessResult());
        roomStatus.setDoorState(record.getDoorState());
        roomStatus.setServoAngle(record.getServoAngle());
        roomStatus.setDoorOpen(record.getDoorOpen());
        roomStatus.setAlarm(record.getAlarm());
        roomStatus.setCooling(record.getCooling());
        roomStatus.setHeating(record.getHeating());
        roomStatus.setHumidifier(record.getHumidifier());
        roomStatus.setDehumidifier(record.getDehumidifier());
        roomStatus.setProjector(record.getProjector());
        roomStatus.setLightOn(record.getLightOn());
        roomStatus.setCurtainOpen(record.getCurtainOpen());
        roomStatus.setBuzzerOn(record.getBuzzerOn());
        roomStatus.setAutoMode(record.getAutoMode());
        roomStatus.setSensorStatus(record.getSensorStatus());
        roomStatus.setErrorMessage(record.getErrorMessage());
        roomStatus.setPayloadJson(record.getPayloadJson());
        roomStatus.setLastReportedAt(record.getReportedAt());
        roomStatus.setCreateTime(now);
        roomStatus.setUpdateTime(now);
        iotRoomStatusMapper.upsert(roomStatus);
    }

    private String buildNormalizedPayloadJson(IotSensorRecord record, String fallbackPayloadJson) {
        try {
            return objectMapper.writeValueAsString(IotTelemetryVo.fromEntity(record));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize normalized telemetry payload: {}", e.getMessage());
            return fallbackPayloadJson;
        }
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? defaultValue : value;
    }

    private Boolean firstNonNull(Boolean... values) {
        for (Boolean value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String deriveTemperatureStatus(BigDecimal temperature) {
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

    private String deriveHumidityStatus(BigDecimal humidity) {
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

    private String deriveLightStatus(Integer light) {
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

    private String deriveSmokeLevel(BigDecimal smoke) {
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

    private boolean isAlertSmokeLevel(String smokeLevel) {
        if (StringUtils.isBlank(smokeLevel)) {
            return false;
        }
        return "danger".equalsIgnoreCase(smokeLevel) || "critical".equalsIgnoreCase(smokeLevel);
    }

    private boolean isDoorOpenState(String doorState) {
        return "open".equalsIgnoreCase(doorState) || "opening".equalsIgnoreCase(doorState);
    }

    private record ResolvedRoom(Meetingroom meetingroom) {
    }
}
