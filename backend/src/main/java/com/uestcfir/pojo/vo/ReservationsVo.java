package com.uestcfir.pojo.vo;

import com.uestcfir.pojo.entity.Reservations;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservationsVo {
    private String roomName;
    private String location;
    private Integer reservationId;
    private Integer roomId;
    private Integer roomNumber;
    private LocalDate reserveDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private String state;
    private String purpose;
    private String username;
    private LocalDateTime approveTime;
    private String rejectReason;
    private String type;
}
