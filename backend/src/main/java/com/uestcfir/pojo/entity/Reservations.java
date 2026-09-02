package com.uestcfir.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 预定实体类
 *
 * @author ylshen
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Reservations {
    private  String roomName;
    private String type;
    private Integer reservationId;
    private Integer userId;
    private Integer approverId;
    private Integer roomNumber;
    private Integer roomId;
    private LocalDate reserveDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer status;//0:待确认，1:已取消 2.以过期 3.使用中 4.已通过 5.已拒绝
    private  String state;
    private String purpose;
    private String username;
    private LocalDateTime approveTime;
    private String rejectReason;
    private String cancelReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String location;



}
