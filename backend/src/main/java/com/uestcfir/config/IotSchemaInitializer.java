package com.uestcfir.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class IotSchemaInitializer implements ApplicationRunner {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS iot_device (
                    device_id VARCHAR(64) PRIMARY KEY,
                    room_code VARCHAR(64) NULL,
                    room_id INT NOT NULL,
                    device_name VARCHAR(100) NULL,
                    device_type VARCHAR(50) NOT NULL DEFAULT 'raspberrypi',
                    protocol VARCHAR(32) NOT NULL DEFAULT 'websocket',
                    online_status VARCHAR(16) NOT NULL DEFAULT 'online',
                    last_seen_at DATETIME NOT NULL,
                    last_reported_at DATETIME NULL,
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_iot_device_room (room_id),
                    INDEX idx_iot_device_room_code (room_code),
                    INDEX idx_iot_device_seen (last_seen_at)
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS iot_sensor_record (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    device_id VARCHAR(64) NOT NULL,
                    room_code VARCHAR(64) NULL,
                    room_id INT NOT NULL,
                    event_type VARCHAR(32) NOT NULL DEFAULT 'telemetry',
                    temperature DECIMAL(6, 2) NULL,
                    humidity DECIMAL(6, 2) NULL,
                    temperature_status VARCHAR(32) NULL,
                    humidity_status VARCHAR(32) NULL,
                    co2_ppm DECIMAL(10, 2) NULL,
                    light INT NULL,
                    light_raw INT NULL,
                    light_status VARCHAR(32) NULL,
                    light_lux DECIMAL(10, 2) NULL,
                    smoke DECIMAL(10, 2) NULL,
                    smoke_raw INT NULL,
                    smoke_level VARCHAR(32) NULL,
                    noise_db DECIMAL(6, 2) NULL,
                    distance_cm DECIMAL(10, 2) NULL,
                    person_near TINYINT(1) NULL,
                    presence TINYINT(1) NULL,
                    last_motion_time DATETIME NULL,
                    people_count INT NULL,
                    motion_detected TINYINT(1) NULL,
                    face_detected TINYINT(1) NULL,
                    face_count INT NULL,
                    authorized TINYINT(1) NULL,
                    access_result VARCHAR(32) NULL,
                    door_state VARCHAR(16) NULL,
                    servo_angle INT NULL,
                    door_open TINYINT(1) NULL,
                    alarm TINYINT(1) NULL,
                    cooling TINYINT(1) NULL,
                    heating TINYINT(1) NULL,
                    humidifier TINYINT(1) NULL,
                    dehumidifier TINYINT(1) NULL,
                    projector TINYINT(1) NULL,
                    light_on TINYINT(1) NULL,
                    curtain_open TINYINT(1) NULL,
                    buzzer_on TINYINT(1) NULL,
                    auto_mode TINYINT(1) NULL,
                    sensor_status VARCHAR(32) NULL,
                    error_message VARCHAR(255) NULL,
                    payload_json TEXT NOT NULL,
                    reported_at DATETIME NOT NULL,
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_iot_room_time (room_id, reported_at),
                    INDEX idx_iot_room_code_time (room_code, reported_at),
                    INDEX idx_iot_device_time (device_id, reported_at)
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS iot_room_status (
                    room_id INT PRIMARY KEY,
                    room_code VARCHAR(64) NULL,
                    device_id VARCHAR(64) NOT NULL,
                    latest_record_id BIGINT NULL,
                    event_type VARCHAR(32) NOT NULL DEFAULT 'telemetry',
                    temperature DECIMAL(6, 2) NULL,
                    humidity DECIMAL(6, 2) NULL,
                    temperature_status VARCHAR(32) NULL,
                    humidity_status VARCHAR(32) NULL,
                    co2_ppm DECIMAL(10, 2) NULL,
                    light INT NULL,
                    light_raw INT NULL,
                    light_status VARCHAR(32) NULL,
                    light_lux DECIMAL(10, 2) NULL,
                    smoke DECIMAL(10, 2) NULL,
                    smoke_raw INT NULL,
                    smoke_level VARCHAR(32) NULL,
                    noise_db DECIMAL(6, 2) NULL,
                    distance_cm DECIMAL(10, 2) NULL,
                    person_near TINYINT(1) NULL,
                    presence TINYINT(1) NULL,
                    last_motion_time DATETIME NULL,
                    people_count INT NULL,
                    motion_detected TINYINT(1) NULL,
                    face_detected TINYINT(1) NULL,
                    face_count INT NULL,
                    authorized TINYINT(1) NULL,
                    access_result VARCHAR(32) NULL,
                    door_state VARCHAR(16) NULL,
                    servo_angle INT NULL,
                    door_open TINYINT(1) NULL,
                    alarm TINYINT(1) NULL,
                    cooling TINYINT(1) NULL,
                    heating TINYINT(1) NULL,
                    humidifier TINYINT(1) NULL,
                    dehumidifier TINYINT(1) NULL,
                    projector TINYINT(1) NULL,
                    light_on TINYINT(1) NULL,
                    curtain_open TINYINT(1) NULL,
                    buzzer_on TINYINT(1) NULL,
                    auto_mode TINYINT(1) NULL,
                    sensor_status VARCHAR(32) NULL,
                    error_message VARCHAR(255) NULL,
                    payload_json TEXT NULL,
                    last_reported_at DATETIME NOT NULL,
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_iot_room_status_code (room_code),
                    INDEX idx_iot_room_status_device (device_id),
                    INDEX idx_iot_room_status_reported (last_reported_at)
                )
                """);

        addColumnIfMissing("iot_device", "room_code", "room_code VARCHAR(64) NULL AFTER device_id");

        addColumnIfMissing("iot_sensor_record", "room_code", "room_code VARCHAR(64) NULL AFTER device_id");
        addColumnIfMissing("iot_sensor_record", "temperature_status", "temperature_status VARCHAR(32) NULL AFTER humidity");
        addColumnIfMissing("iot_sensor_record", "humidity_status", "humidity_status VARCHAR(32) NULL AFTER temperature_status");
        addColumnIfMissing("iot_sensor_record", "light", "light INT NULL AFTER co2_ppm");
        addColumnIfMissing("iot_sensor_record", "light_raw", "light_raw INT NULL AFTER light");
        addColumnIfMissing("iot_sensor_record", "light_status", "light_status VARCHAR(32) NULL AFTER light_raw");
        addColumnIfMissing("iot_sensor_record", "smoke", "smoke DECIMAL(10, 2) NULL AFTER light_lux");
        addColumnIfMissing("iot_sensor_record", "smoke_raw", "smoke_raw INT NULL AFTER smoke");
        addColumnIfMissing("iot_sensor_record", "smoke_level", "smoke_level VARCHAR(32) NULL AFTER smoke_raw");
        addColumnIfMissing("iot_sensor_record", "person_near", "person_near TINYINT(1) NULL AFTER distance_cm");
        addColumnIfMissing("iot_sensor_record", "presence", "presence TINYINT(1) NULL AFTER person_near");
        addColumnIfMissing("iot_sensor_record", "last_motion_time", "last_motion_time DATETIME NULL AFTER presence");
        addColumnIfMissing("iot_sensor_record", "face_detected", "face_detected TINYINT(1) NULL AFTER motion_detected");
        addColumnIfMissing("iot_sensor_record", "face_count", "face_count INT NULL AFTER face_detected");
        addColumnIfMissing("iot_sensor_record", "authorized", "authorized TINYINT(1) NULL AFTER face_count");
        addColumnIfMissing("iot_sensor_record", "access_result", "access_result VARCHAR(32) NULL AFTER authorized");
        addColumnIfMissing("iot_sensor_record", "door_state", "door_state VARCHAR(16) NULL AFTER access_result");
        addColumnIfMissing("iot_sensor_record", "servo_angle", "servo_angle INT NULL AFTER door_state");
        addColumnIfMissing("iot_sensor_record", "alarm", "alarm TINYINT(1) NULL AFTER door_open");
        addColumnIfMissing("iot_sensor_record", "cooling", "cooling TINYINT(1) NULL AFTER alarm");
        addColumnIfMissing("iot_sensor_record", "heating", "heating TINYINT(1) NULL AFTER cooling");
        addColumnIfMissing("iot_sensor_record", "humidifier", "humidifier TINYINT(1) NULL AFTER heating");
        addColumnIfMissing("iot_sensor_record", "dehumidifier", "dehumidifier TINYINT(1) NULL AFTER humidifier");
        addColumnIfMissing("iot_sensor_record", "projector", "projector TINYINT(1) NULL AFTER dehumidifier");
        addColumnIfMissing("iot_sensor_record", "light_on", "light_on TINYINT(1) NULL AFTER dehumidifier");
        addColumnIfMissing("iot_sensor_record", "curtain_open", "curtain_open TINYINT(1) NULL AFTER light_on");
        addColumnIfMissing("iot_sensor_record", "buzzer_on", "buzzer_on TINYINT(1) NULL AFTER curtain_open");
        addColumnIfMissing("iot_sensor_record", "auto_mode", "auto_mode TINYINT(1) NULL AFTER buzzer_on");
        addColumnIfMissing("iot_sensor_record", "sensor_status", "sensor_status VARCHAR(32) NULL AFTER buzzer_on");
        addColumnIfMissing("iot_sensor_record", "error_message", "error_message VARCHAR(255) NULL AFTER sensor_status");

        addColumnIfMissing("iot_room_status", "room_code", "room_code VARCHAR(64) NULL AFTER room_id");
        addColumnIfMissing("iot_room_status", "temperature_status", "temperature_status VARCHAR(32) NULL AFTER humidity");
        addColumnIfMissing("iot_room_status", "humidity_status", "humidity_status VARCHAR(32) NULL AFTER temperature_status");
        addColumnIfMissing("iot_room_status", "light", "light INT NULL AFTER co2_ppm");
        addColumnIfMissing("iot_room_status", "light_raw", "light_raw INT NULL AFTER light");
        addColumnIfMissing("iot_room_status", "light_status", "light_status VARCHAR(32) NULL AFTER light_raw");
        addColumnIfMissing("iot_room_status", "smoke", "smoke DECIMAL(10, 2) NULL AFTER light_lux");
        addColumnIfMissing("iot_room_status", "smoke_raw", "smoke_raw INT NULL AFTER smoke");
        addColumnIfMissing("iot_room_status", "smoke_level", "smoke_level VARCHAR(32) NULL AFTER smoke_raw");
        addColumnIfMissing("iot_room_status", "person_near", "person_near TINYINT(1) NULL AFTER distance_cm");
        addColumnIfMissing("iot_room_status", "presence", "presence TINYINT(1) NULL AFTER person_near");
        addColumnIfMissing("iot_room_status", "last_motion_time", "last_motion_time DATETIME NULL AFTER presence");
        addColumnIfMissing("iot_room_status", "face_detected", "face_detected TINYINT(1) NULL AFTER motion_detected");
        addColumnIfMissing("iot_room_status", "face_count", "face_count INT NULL AFTER face_detected");
        addColumnIfMissing("iot_room_status", "authorized", "authorized TINYINT(1) NULL AFTER face_count");
        addColumnIfMissing("iot_room_status", "access_result", "access_result VARCHAR(32) NULL AFTER authorized");
        addColumnIfMissing("iot_room_status", "door_state", "door_state VARCHAR(16) NULL AFTER access_result");
        addColumnIfMissing("iot_room_status", "servo_angle", "servo_angle INT NULL AFTER door_state");
        addColumnIfMissing("iot_room_status", "alarm", "alarm TINYINT(1) NULL AFTER door_open");
        addColumnIfMissing("iot_room_status", "cooling", "cooling TINYINT(1) NULL AFTER alarm");
        addColumnIfMissing("iot_room_status", "heating", "heating TINYINT(1) NULL AFTER cooling");
        addColumnIfMissing("iot_room_status", "humidifier", "humidifier TINYINT(1) NULL AFTER heating");
        addColumnIfMissing("iot_room_status", "dehumidifier", "dehumidifier TINYINT(1) NULL AFTER humidifier");
        addColumnIfMissing("iot_room_status", "projector", "projector TINYINT(1) NULL AFTER dehumidifier");
        addColumnIfMissing("iot_room_status", "light_on", "light_on TINYINT(1) NULL AFTER dehumidifier");
        addColumnIfMissing("iot_room_status", "curtain_open", "curtain_open TINYINT(1) NULL AFTER light_on");
        addColumnIfMissing("iot_room_status", "buzzer_on", "buzzer_on TINYINT(1) NULL AFTER curtain_open");
        addColumnIfMissing("iot_room_status", "auto_mode", "auto_mode TINYINT(1) NULL AFTER buzzer_on");
        addColumnIfMissing("iot_room_status", "sensor_status", "sensor_status VARCHAR(32) NULL AFTER buzzer_on");
        addColumnIfMissing("iot_room_status", "error_message", "error_message VARCHAR(255) NULL AFTER sensor_status");
        addColumnIfMissing("iot_room_status", "payload_json", "payload_json TEXT NULL AFTER error_message");

        log.info("IoT schema ready: iot_device, iot_sensor_record, iot_room_status");
    }

    private void addColumnIfMissing(String tableName, String columnName, String columnDefinition) {
        Integer columnCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """, Integer.class, tableName, columnName);

        if (columnCount != null && columnCount == 0) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnDefinition);
        }
    }
}
