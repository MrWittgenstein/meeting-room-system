package com.uestcfir.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会议室使用次数排名DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoomUsageRankDTO {
    // 排名
    private Integer rank;

    // 会议室ID
    private Integer roomId;

    // 会议室名称
    private String roomName;

    // 使用次数
    private Integer usageCount;

    // 上周使用次数（用于对比）
    private Integer lastWeekUsageCount;

    // 使用次数变化（百分比）
    private Double changePercentage;

    // 使用率（占全部会议室的比例）
    private Double usageRate;

    // 会议室类型
    private String roomType;

    // 会议室容量
    private Integer capacity;

    // 位置
    private String location;
}