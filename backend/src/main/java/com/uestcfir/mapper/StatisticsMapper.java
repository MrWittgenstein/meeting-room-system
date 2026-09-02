package com.uestcfir.mapper;

import com.uestcfir.pojo.dto.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface StatisticsMapper {

    /**
     * 获取会议室总数
     */
    Integer countTotalMeetingRooms();

    /**
     * 获取待审核申请数量（status = 0）
     */
    Integer countPendingApplications();

    /**
     * 获取已审核申请数量（status = 4 或 1）
     */
    Integer countReviewedApplications();

    /**
     * 获取今日会议数量（reserve_date = 今天，status = 4）
     */
    Integer countTodayMeetings(@Param("today") LocalDate today);

    /**
     * 获取今日会议详情列表
     */
    List<TodayMeetingDTO> getTodayMeetingList(@Param("today") LocalDate today);

    /**
     * 获取今日待审核申请数量
     */
    Integer countTodayPendingApplications(@Param("today") LocalDate today);

    /**
     * 获取今日已通过的会议数量
     */
    Integer countTodayApprovedMeetings(@Param("today") LocalDate today);

    /**
     * 获取会议室使用频率图表数据
     */
    RoomFrequencyChartDTO getRoomFrequencyChart(@Param("query") FrequencyQueryDTO query);

    /**
     * 获取会议室使用次数排名
     */
    List<RoomUsageRankDTO> getRoomUsageRank(@Param("query") FrequencyQueryDTO query);

    /**
     * 获取时间段内的会议室使用次数
     */
    List<Map<String, Object>> getRoomUsageByPeriod(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("roomIds") List<Integer> roomIds
    );

    /**
     * 获取会议室上周使用次数（用于对比）
     */
    Map<Integer, Integer> getLastWeekUsageCount(
            @Param("roomIds") List<Integer> roomIds,
            @Param("currentWeekStart") LocalDate currentWeekStart,
            @Param("currentWeekEnd") LocalDate currentWeekEnd
    );

    /**
     * 获取统计周期的开始和结束日期
     */
    Map<String, LocalDate> getPeriodDateRange(@Param("period") String period);

    /**
     * 获取会议室基本信息列表
     */
    List<Map<String, Object>> getRoomBasicInfo(@Param("roomType") String roomType,
                                               @Param("location") String location);
}