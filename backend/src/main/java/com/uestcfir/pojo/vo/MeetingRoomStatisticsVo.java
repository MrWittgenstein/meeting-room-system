package com.uestcfir.pojo.vo;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class MeetingRoomStatisticsVo {
    // 基础统计
    private Integer totalRooms;
    private Integer reviewedApplications;
    private Integer pendingApplications;

    // 按类型分类的统计
    private Map<String, Integer> todayApprovedByType;
    private Map<String, Integer> weekApprovedByType;
    private Map<String, Integer> monthApprovedByType;
    private Map<String, Integer> todayApplicationsByType;
    private Map<String, Integer> weekApplicationsByType;
    private Map<String, Integer> monthApplicationsByType;

    // 构造函数初始化Map
    public MeetingRoomStatisticsVo() {
        this.todayApprovedByType = new HashMap<>();
        this.weekApprovedByType = new HashMap<>();
        this.monthApprovedByType = new HashMap<>();
        this.todayApplicationsByType = new HashMap<>();
        this.weekApplicationsByType = new HashMap<>();
        this.monthApplicationsByType = new HashMap<>();
    }
}