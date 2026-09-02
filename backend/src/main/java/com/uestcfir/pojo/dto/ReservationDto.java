package com.uestcfir.pojo.dto;


import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class ReservationDto {
    private String roomName;
    private String type;
    private String location;
    private Integer reservationId;
    private Integer roomId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String purpose;
    private Integer status;
    private Integer roomNumber;
    private String state;
    private String localtion;
    private Integer page = 1;
    private Integer size = 10;


}
