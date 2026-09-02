package com.uestcfir.pojo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class IotTelemetryRequest {
    private String deviceId;
    private String roomCode;
    private Integer roomId;
    private String eventType;
    private BigDecimal temperature;
    private BigDecimal humidity;
    private String temperatureStatus;
    private String humidityStatus;
    private BigDecimal co2Ppm;
    private Integer light;
    private Integer lightRaw;
    private String lightStatus;
    private BigDecimal lightLux;
    private BigDecimal smoke;
    private Integer smokeRaw;
    private String smokeLevel;
    private BigDecimal noiseDb;
    private BigDecimal distanceCm;
    private Boolean personNear;
    private Boolean presence;
    private LocalDateTime lastMotionTime;
    private Integer peopleCount;
    private Boolean motionDetected;
    private Boolean faceDetected;
    private Integer faceCount;
    private Boolean authorized;
    private String accessResult;
    private String doorState;
    private Integer servoAngle;
    private Boolean doorOpen;
    private Boolean alarm;
    private Boolean cooling;
    private Boolean heating;
    private Boolean humidifier;
    private Boolean dehumidifier;
    private Boolean projector;
    private Boolean lightOn;
    private Boolean curtainOpen;
    private Boolean buzzerOn;
    private Boolean autoMode;
    private String sensorStatus;
    private String errorMessage;
    private LocalDateTime reportedAt;
}
