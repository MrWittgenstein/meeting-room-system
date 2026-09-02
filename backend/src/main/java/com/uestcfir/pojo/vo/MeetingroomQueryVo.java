package com.uestcfir.pojo.vo;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
public class MeetingroomQueryVo {
    private Integer roomId;
    private String roomName;
     private Integer roomNumber;
     private Integer capacity;
     private String location;
     private Integer status;//0可用，1维修中
    private LocalTime openTime;
    private LocalTime closeTime;
    private String type;//会议室类型
    private String originImage;//原图
    private Boolean isAvailable;//是否可预约
    private List<TimeSlot> availableSlots;

    @Data
    @AllArgsConstructor
    public static class TimeSlot {
        private LocalTime startTime;
        private LocalTime endTime;
    }
}

