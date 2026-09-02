package com.uestcfir.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 会议室使用频率统计DTO（用于柱状图）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoomFrequencyChartDTO {
    // 统计周期：week（本周）、month（本月）、quarter（本季度）、year（本年）
    private String period;

    // 开始日期
    private String startDate;

    // 结束日期
    private String endDate;

    // 会议室名称列表
    private List<String> roomNames;

    // 使用次数列表（与roomNames一一对应）
    private List<Integer> usageCounts;

    // 总使用次数
    private Integer totalUsageCount;

    // 平均使用次数
    private Double averageUsageCount;

    // 最高使用次数
    private Integer maxUsageCount;

    // 最低使用次数
    private Integer minUsageCount;

    // 统计时间
    private String statTime;
}


