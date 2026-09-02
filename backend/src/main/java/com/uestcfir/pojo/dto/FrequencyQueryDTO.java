package com.uestcfir.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 频率统计查询DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FrequencyQueryDTO {
    // 统计周期：week、month、quarter、year、custom
    private String period;

    // 开始日期（当period=custom时使用）
    private String startDate;

    // 结束日期（当period=custom时使用）
    private String endDate;

    // 排序方式：count_desc（次数降序）、count_asc（次数升序）、name（名称排序）
    private String sortBy;

    // 限制返回数量（0表示全部）
    private Integer limit;

    // 是否包含零使用率的会议室
    private Boolean includeZeroUsage;

    // 会议室类型筛选
    private String roomType;

    // 位置筛选
    private String location;
}
