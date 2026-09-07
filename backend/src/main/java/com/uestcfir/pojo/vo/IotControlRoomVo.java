package com.uestcfir.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A meeting room that has an IoT device available for control.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IotControlRoomVo {
    private Integer roomId;
    private Integer roomNumber;
    private String roomName;
    private Integer roomStatus;
    private String deviceId;
}
