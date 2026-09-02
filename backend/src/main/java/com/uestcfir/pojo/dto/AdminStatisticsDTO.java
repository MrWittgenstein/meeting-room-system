package com.uestcfir.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 管理员统计量DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminStatisticsDTO {
    // 会议室总数
    private Integer totalMeetingRooms;

    // 待审核申请数量
    private Integer pendingApplications;

    // 已审核申请数量
    private Integer reviewedApplications;

    // 今日会议数量
    private Integer todayMeetings;

    // 今日会议详情列表
    private List<TodayMeetingDTO> todayMeetingList;

    // 可选：今日通过申请的会议
    private Integer todayApprovedMeetings;

    // 可选：今日待审核申请
    private Integer todayPendingApplications;
}