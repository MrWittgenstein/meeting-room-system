package com.uestcfir.pojo.dto;


import com.uestcfir.exception.ValidationException;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.time.LocalTime;

@Data
public class MeetingroomQueryDto {
    // 时间段查询
    private LocalDate queryDate;           // 查询日期
    private LocalTime startTime;           // 开始时间
    private LocalTime endTime;             // 结束时间

    // 会议室基础条件
    private Integer minCapacity;
    private Integer maxCapacity;
    private String location;

    // 分页参数
    private Integer page = 1;
    private Integer size = 10;



    // 验证方法
    public boolean hasTimeRange() {
        return startTime != null && endTime != null;
    }

    public void validateTimeRange() {
        if (hasTimeRange() && !startTime.isBefore(endTime)) {
            throw new ValidationException("开始时间必须早于结束时间");
        }
    }

}
