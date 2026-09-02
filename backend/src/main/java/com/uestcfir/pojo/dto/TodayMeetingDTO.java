package com.uestcfir.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 今日会议详情DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TodayMeetingDTO {
    // 会议室名称
    private String roomName;

    // 开始时间
    private java.time.LocalTime startTime;

    // 预约日期
    private java.time.LocalDate reserveDate;

    // 会议目的
    private String purpose;

    // 申请人用户名
    private String username;

    // 状态（前端可显示状态描述）
    private Integer status;

    // 状态描述
    private String statusDesc;
}