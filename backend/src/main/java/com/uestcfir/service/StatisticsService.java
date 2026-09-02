package com.uestcfir.service;

import com.uestcfir.pojo.dto.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface StatisticsService {

    /**
     * 获取管理员统计信息
     */
    AdminStatisticsDTO getAdminStatistics();


    /**
     * 获取会议室使用频率图表数据（用于生成柱状图）
     */
    RoomFrequencyChartDTO getRoomFrequencyChart(FrequencyQueryDTO query);

    /**
     * 获取会议室使用次数排名
     */
    List<RoomUsageRankDTO> getRoomUsageRank(FrequencyQueryDTO query);

    /**
     * 获取会议室使用频率统计看板（包含图表、排名、摘要等）
     */
    Map<String, Object> getRoomFrequencyDashboard(FrequencyQueryDTO query);

    /**
     * 获取两个时间段的会议室使用频率对比
     */
    Map<String, Object> getRoomFrequencyComparison(FrequencyQueryDTO query1, FrequencyQueryDTO query2);
}