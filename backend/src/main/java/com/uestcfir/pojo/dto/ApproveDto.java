package com.uestcfir.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDateTime;

@Data
public class ApproveDto {
    @NotBlank(message = "预约Id不能为空")
    private Integer reservationId;
    private Integer approveStatus;
    private String rejectReason;
    private LocalDateTime approveTime;
}
