package com.uestcfir.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uestcfir.exception.BusinessException;
import com.uestcfir.pojo.dto.IotDeviceCommandRequest;
import com.uestcfir.pojo.dto.IotTelemetryRequest;
import com.uestcfir.pojo.entity.IotSensorRecord;
import com.uestcfir.pojo.vo.IotDeviceCommandResultVo;
import com.uestcfir.pojo.vo.IotRealtimeMessageVo;
import com.uestcfir.pojo.vo.IotTelemetryVo;
import com.uestcfir.service.IotTelemetryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
public class IotDeviceWebSocketHandler extends TextWebSocketHandler {
    private static final DateTimeFormatter[] TIME_FORMATTERS = new DateTimeFormatter[]{
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
    };

    @Autowired
    private IotTelemetryService iotTelemetryService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final Map<String, WebSocketSession> deviceSessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionDeviceIds = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Map<String, Object>>> pendingCommands = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("IoT device websocket connected. sessionId={}, remote={}", session.getId(), session.getRemoteAddress());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(buildResponse("CONNECTED", "success", "iot websocket connected", null))));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        try {
            if (handleDeviceAck(payload)) {
                return;
            }
            IotTelemetryRequest request = parsePayload(payload);
            IotSensorRecord record = iotTelemetryService.saveTelemetry(request, payload);
            bindDeviceSession(record.getDeviceId(), session);
            publishTelemetry(record);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(buildResponse("ACK", "success", "telemetry saved", record))));
        } catch (BusinessException | IllegalArgumentException e) {
            log.warn("Invalid IoT payload: {}", e.getMessage());
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(buildResponse("ACK", "error", e.getMessage(), null))));
        } catch (JsonProcessingException e) {
            log.warn("IoT payload is not valid json: {}", e.getMessage());
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(buildResponse("ACK", "error", "payload must be valid json", null))));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("IoT websocket transport error. sessionId={}", session.getId(), exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        unbindDeviceSession(session);
        log.info("IoT device websocket closed. sessionId={}, status={}", session.getId(), status);
    }

    public IotDeviceCommandResultVo sendCommand(String deviceId, IotDeviceCommandRequest request) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new BusinessException("deviceId can not be empty");
        }
        if (request == null || request.getCommand() == null || request.getCommand().isBlank()) {
            throw new BusinessException("command can not be empty");
        }

        WebSocketSession session = deviceSessions.get(deviceId.trim());
        if (session == null || !session.isOpen()) {
            throw new BusinessException("IoT device is offline, deviceId=" + deviceId);
        }

        String commandId = UUID.randomUUID().toString();
        Map<String, Object> commandPayload = new LinkedHashMap<>();
        commandPayload.put("type", "COMMAND");
        commandPayload.put("commandId", commandId);
        commandPayload.put("deviceId", deviceId.trim());
        commandPayload.put("command", request.getCommand().trim());
        commandPayload.put("target", request.getTarget());
        commandPayload.put("value", request.getValue());
        commandPayload.put("angle", request.getAngle());
        commandPayload.put("params", request.getParams());
        commandPayload.put("timestamp", LocalDateTime.now());

        CompletableFuture<Map<String, Object>> ackFuture = new CompletableFuture<>();
        pendingCommands.put(commandId, ackFuture);
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(commandPayload)));
            }
            Map<String, Object> ack = ackFuture.get(3, TimeUnit.SECONDS);
            String status = String.valueOf(ack.getOrDefault("status", "unknown"));
            String message = String.valueOf(ack.getOrDefault("message", "command acknowledged"));
            return new IotDeviceCommandResultVo(commandId, deviceId.trim(), status, message, ack);
        } catch (TimeoutException e) {
            throw new BusinessException("IoT device did not acknowledge command in time, deviceId=" + deviceId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Failed to send IoT command: " + e.getMessage());
        } finally {
            pendingCommands.remove(commandId);
        }
    }

    private void publishTelemetry(IotSensorRecord record) {
        IotTelemetryVo current = IotTelemetryVo.fromEntity(record);
        List<IotTelemetryVo> history = new ArrayList<>(iotTelemetryService.getRecentByRoomId(record.getRoomId(), 50)
                .stream()
                .map(IotTelemetryVo::fromEntity)
                .toList());
        Collections.reverse(history);

        IotRealtimeMessageVo message = new IotRealtimeMessageVo(current, history);
        String roomTopic = current.getRoomId() == null ? String.valueOf(record.getRoomId()) : current.getRoomId();
        messagingTemplate.convertAndSend("/topic/iot/rooms/" + roomTopic, message);
        messagingTemplate.convertAndSend("/topic/iot/devices/" + record.getDeviceId(), message);
    }

    private boolean handleDeviceAck(String payload) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(payload);
        String type = readText(new String[]{"type"}, root);
        if (!"COMMAND_ACK".equalsIgnoreCase(type)) {
            return false;
        }

        String commandId = readText(new String[]{"commandId", "command_id"}, root);
        if (commandId == null || commandId.isBlank()) {
            return true;
        }

        CompletableFuture<Map<String, Object>> future = pendingCommands.get(commandId);
        if (future != null) {
            Map<String, Object> ack = objectMapper.convertValue(root, new TypeReference<>() {
            });
            future.complete(ack);
        }
        return true;
    }

    private void bindDeviceSession(String deviceId, WebSocketSession session) {
        if (deviceId == null || deviceId.isBlank()) {
            return;
        }
        String normalizedDeviceId = deviceId.trim();
        deviceSessions.put(normalizedDeviceId, session);
        sessionDeviceIds.put(session.getId(), normalizedDeviceId);
    }

    private void unbindDeviceSession(WebSocketSession session) {
        String deviceId = sessionDeviceIds.remove(session.getId());
        if (deviceId != null) {
            deviceSessions.remove(deviceId, session);
        }
    }

    private IotTelemetryRequest parsePayload(String payload) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(payload);
        JsonNode currentNode = root.has("current") && root.get("current").isObject() ? root.get("current") : root;
        JsonNode environmentNode = childObject(currentNode, "environment");
        JsonNode presenceNode = childObject(currentNode, "presence");
        JsonNode accessControlNode = childObject(currentNode, "access_control");
        JsonNode devicesNode = childObject(currentNode, "devices");
        JsonNode systemNode = childObject(currentNode, "system");

        IotTelemetryRequest request = new IotTelemetryRequest();
        request.setDeviceId(readText(new String[]{"device_id", "deviceId"}, root, currentNode));
        request.setRoomCode(readText(new String[]{"room_id", "roomCode"}, currentNode, root));
        request.setRoomId(readInteger(new String[]{"roomId", "meetingRoomId", "meeting_room_id"}, currentNode, root));
        request.setEventType(readText(new String[]{"event_type", "eventType"}, currentNode, root));
        request.setTemperature(readDecimal(new String[]{"temperature"}, environmentNode, currentNode, root));
        request.setHumidity(readDecimal(new String[]{"humidity"}, environmentNode, currentNode, root));
        request.setTemperatureStatus(readText(new String[]{"temperature_status", "temperatureStatus"}, environmentNode, currentNode, root));
        request.setHumidityStatus(readText(new String[]{"humidity_status", "humidityStatus"}, environmentNode, currentNode, root));
        request.setCo2Ppm(readDecimal(new String[]{"co2_ppm", "co2Ppm", "co2"}, environmentNode, currentNode, root));
        request.setLight(readInteger(new String[]{"light"}, environmentNode, currentNode, root));
        request.setLightRaw(readInteger(new String[]{"light_raw", "lightRaw"}, environmentNode, currentNode, root));
        request.setLightStatus(readText(new String[]{"light_status", "lightStatus"}, environmentNode, currentNode, root));
        request.setLightLux(readDecimal(new String[]{"light_lux", "lightLux", "lux"}, environmentNode, currentNode, root));
        request.setSmoke(readDecimal(new String[]{"smoke"}, environmentNode, currentNode, root));
        request.setSmokeRaw(readInteger(new String[]{"smoke_raw", "smokeRaw"}, environmentNode, currentNode, root));
        request.setSmokeLevel(readText(new String[]{"smoke_level", "smokeLevel"}, environmentNode, currentNode, root));
        request.setNoiseDb(readDecimal(new String[]{"noise_db", "noiseDb", "noise"}, environmentNode, currentNode, root));
        request.setDistanceCm(readDecimal(new String[]{"distance", "distanceCm"}, presenceNode, currentNode, root));
        request.setPersonNear(readBoolean(new String[]{"person_near", "personNear"}, presenceNode, currentNode, root));
        request.setPresence(readBoolean(new String[]{"presence", "pir"}, presenceNode, currentNode, root));
        request.setLastMotionTime(resolveDateTime(new String[]{"last_motion_time", "lastMotionTime"}, presenceNode, currentNode, root));
        request.setPeopleCount(readInteger(new String[]{"people_count", "peopleCount"}, presenceNode, currentNode, root));
        request.setMotionDetected(readBoolean(new String[]{"motion_detected", "motionDetected", "presence", "pir"}, presenceNode, currentNode, root));
        request.setFaceDetected(readBoolean(new String[]{"face_detected", "faceDetected"}, accessControlNode, currentNode, root));
        request.setFaceCount(readInteger(new String[]{"face_count", "faceCount"}, accessControlNode, currentNode, root));
        request.setAuthorized(readBoolean(new String[]{"authorized"}, accessControlNode, currentNode, root));
        request.setAccessResult(readText(new String[]{"access_result", "accessResult"}, accessControlNode, currentNode, root));
        request.setDoorState(readText(new String[]{"door_state", "doorState"}, accessControlNode, currentNode, root));
        request.setServoAngle(readInteger(new String[]{"servo_angle", "servoAngle"}, accessControlNode, currentNode, root));
        request.setDoorOpen(readBoolean(new String[]{"door_open", "doorOpen", "door_state", "doorState"}, accessControlNode, currentNode, root));
        request.setAlarm(readBoolean(new String[]{"alarm"}, devicesNode, currentNode, root));
        request.setCooling(readBoolean(new String[]{"cooling"}, devicesNode, currentNode, root));
        request.setHeating(readBoolean(new String[]{"heating"}, devicesNode, currentNode, root));
        request.setHumidifier(readBoolean(new String[]{"humidifier"}, devicesNode, currentNode, root));
        request.setDehumidifier(readBoolean(new String[]{"dehumidifier"}, devicesNode, currentNode, root));
        request.setProjector(readBoolean(new String[]{"projector", "projector_on", "projectorOn"}, devicesNode, currentNode, root));
        request.setLightOn(readBoolean(new String[]{"light_on", "lightOn"}, devicesNode, currentNode, root));
        request.setCurtainOpen(readBoolean(new String[]{"curtain_open", "curtainOpen"}, devicesNode, currentNode, root));
        request.setBuzzerOn(readBoolean(new String[]{"buzzer_on", "buzzerOn"}, devicesNode, currentNode, root));
        request.setAutoMode(readBoolean(new String[]{"auto_mode", "autoMode", "auto"}, devicesNode, currentNode, root));
        request.setSensorStatus(readText(new String[]{"sensor_status", "sensorStatus"}, systemNode, currentNode, root));
        request.setErrorMessage(readText(new String[]{"error", "errorMessage"}, systemNode, currentNode, root));
        request.setReportedAt(resolveReportedAt(currentNode, root));
        return request;
    }

    private Map<String, Object> buildResponse(String type, String status, String message, IotSensorRecord record) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("type", type);
        response.put("status", status);
        response.put("message", message);
        if (record != null) {
            response.put("recordId", record.getId());
            response.put("deviceId", record.getDeviceId());
            response.put("roomId", record.getRoomId());
            response.put("roomCode", record.getRoomCode());
            response.put("reportedAt", record.getReportedAt());
        }
        return response;
    }

    private JsonNode childObject(JsonNode parent, String fieldName) {
        if (parent != null && parent.has(fieldName) && parent.get(fieldName).isObject()) {
            return parent.get(fieldName);
        }
        return null;
    }

    private String readText(String[] fieldNames, JsonNode... nodes) {
        JsonNode node = findNode(fieldNames, nodes);
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return value == null ? null : value.trim();
    }

    private Integer readInteger(String[] fieldNames, JsonNode... nodes) {
        JsonNode node = findNode(fieldNames, nodes);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isIntegralNumber()) {
            return node.asInt();
        }
        String text = node.asText().trim();
        if (text.isEmpty()) {
            return null;
        }
        return Integer.valueOf(text);
    }

    private BigDecimal readDecimal(String[] fieldNames, JsonNode... nodes) {
        JsonNode node = findNode(fieldNames, nodes);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        String text = node.asText().trim();
        if (text.isEmpty()) {
            return null;
        }
        return new BigDecimal(text);
    }

    private Boolean readBoolean(String[] fieldNames, JsonNode... nodes) {
        JsonNode node = findNode(fieldNames, nodes);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        String text = node.asText().trim().toLowerCase();
        if (text.isEmpty()) {
            return null;
        }
        if ("1".equals(text) || "true".equals(text) || "yes".equals(text) || "present".equals(text) || "on".equals(text) || "open".equals(text)) {
            return true;
        }
        if ("0".equals(text) || "false".equals(text) || "no".equals(text) || "absent".equals(text) || "off".equals(text) || "closed".equals(text)) {
            return false;
        }
        return null;
    }

    private LocalDateTime resolveReportedAt(JsonNode... nodes) {
        return resolveDateTime(new String[]{"timestamp", "reportedAt", "reported_at"}, nodes);
    }

    private LocalDateTime resolveDateTime(String[] fieldNames, JsonNode... nodes) {
        JsonNode node = findNode(fieldNames, nodes);
        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isIntegralNumber()) {
            long timestamp = node.asLong();
            Instant instant = timestamp > 9999999999L ? Instant.ofEpochMilli(timestamp) : Instant.ofEpochSecond(timestamp);
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        }

        String text = node.asText().trim();
        if (text.isEmpty()) {
            return null;
        }

        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        throw new IllegalArgumentException("timestamp format is not supported");
    }

    private JsonNode findNode(String[] fieldNames, JsonNode... nodes) {
        for (String fieldName : fieldNames) {
            for (JsonNode node : nodes) {
                if (node != null && node.has(fieldName)) {
                    return node.get(fieldName);
                }
            }
        }
        return null;
    }
}
