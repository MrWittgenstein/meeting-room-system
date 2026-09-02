package com.uestcfir.mapper;

import com.uestcfir.pojo.entity.IotDevice;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface IotDeviceMapper {
    @Insert("""
            INSERT INTO iot_device (
                device_id,
                room_code,
                room_id,
                device_name,
                device_type,
                protocol,
                online_status,
                last_seen_at,
                last_reported_at,
                create_time,
                update_time
            ) VALUES (
                #{deviceId},
                #{roomCode},
                #{roomId},
                #{deviceName},
                #{deviceType},
                #{protocol},
                #{onlineStatus},
                #{lastSeenAt},
                #{lastReportedAt},
                #{createTime},
                #{updateTime}
            )
            ON DUPLICATE KEY UPDATE
                room_code = VALUES(room_code),
                room_id = VALUES(room_id),
                device_name = COALESCE(VALUES(device_name), device_name),
                device_type = VALUES(device_type),
                protocol = VALUES(protocol),
                online_status = VALUES(online_status),
                last_seen_at = VALUES(last_seen_at),
                last_reported_at = VALUES(last_reported_at),
                update_time = VALUES(update_time)
            """)
    int upsert(IotDevice device);

    @Select("""
            SELECT *
            FROM iot_device
            WHERE device_id = #{deviceId}
            """)
    IotDevice findByDeviceId(@Param("deviceId") String deviceId);
}
