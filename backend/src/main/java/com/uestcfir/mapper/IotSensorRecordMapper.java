package com.uestcfir.mapper;

import com.uestcfir.pojo.entity.IotSensorRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface IotSensorRecordMapper {
    @Insert("""
            INSERT INTO iot_sensor_record (
                device_id,
                room_code,
                room_id,
                event_type,
                temperature,
                humidity,
                temperature_status,
                humidity_status,
                co2_ppm,
                light,
                light_raw,
                light_status,
                light_lux,
                smoke,
                smoke_raw,
                smoke_level,
                noise_db,
                distance_cm,
                person_near,
                presence,
                last_motion_time,
                people_count,
                motion_detected,
                face_detected,
                face_count,
                authorized,
                access_result,
                door_state,
                servo_angle,
                door_open,
                alarm,
                cooling,
                heating,
                humidifier,
                dehumidifier,
                projector,
                light_on,
                curtain_open,
                buzzer_on,
                auto_mode,
                sensor_status,
                error_message,
                payload_json,
                reported_at,
                create_time,
                update_time
            ) VALUES (
                #{deviceId},
                #{roomCode},
                #{roomId},
                #{eventType},
                #{temperature},
                #{humidity},
                #{temperatureStatus},
                #{humidityStatus},
                #{co2Ppm},
                #{light},
                #{lightRaw},
                #{lightStatus},
                #{lightLux},
                #{smoke},
                #{smokeRaw},
                #{smokeLevel},
                #{noiseDb},
                #{distanceCm},
                #{personNear},
                #{presence},
                #{lastMotionTime},
                #{peopleCount},
                #{motionDetected},
                #{faceDetected},
                #{faceCount},
                #{authorized},
                #{accessResult},
                #{doorState},
                #{servoAngle},
                #{doorOpen},
                #{alarm},
                #{cooling},
                #{heating},
                #{humidifier},
                #{dehumidifier},
                #{projector},
                #{lightOn},
                #{curtainOpen},
                #{buzzerOn},
                #{autoMode},
                #{sensorStatus},
                #{errorMessage},
                #{payloadJson},
                #{reportedAt},
                #{createTime},
                #{updateTime}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(IotSensorRecord record);

    @Select("""
            SELECT *
            FROM iot_sensor_record
            WHERE room_id = #{roomId}
            ORDER BY reported_at DESC, id DESC
            LIMIT 1
            """)
    IotSensorRecord findLatestByRoomId(@Param("roomId") Integer roomId);

    @Select("""
            SELECT *
            FROM iot_sensor_record
            WHERE room_id = #{roomId}
            ORDER BY reported_at DESC, id DESC
            LIMIT #{limit}
            """)
    List<IotSensorRecord> findRecentByRoomId(@Param("roomId") Integer roomId, @Param("limit") Integer limit);

    @Select("""
            SELECT *
            FROM iot_sensor_record
            WHERE device_id = #{deviceId}
            ORDER BY reported_at DESC, id DESC
            LIMIT 1
            """)
    IotSensorRecord findLatestByDeviceId(@Param("deviceId") String deviceId);
}
