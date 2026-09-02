package com.uestcfir.service.impl;



import com.uestcfir.mapper.StatisticsMapper;
import com.uestcfir.pojo.dto.*;
import com.uestcfir.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final StatisticsMapper statisticsMapper;

    @Override
    public AdminStatisticsDTO getAdminStatistics() {
        LocalDate today = LocalDate.now();

        // 获取各项统计数据
        Integer totalMeetingRooms = statisticsMapper.countTotalMeetingRooms();
        Integer pendingApplications = statisticsMapper.countPendingApplications();
        Integer reviewedApplications = statisticsMapper.countReviewedApplications();
        Integer todayMeetings = statisticsMapper.countTodayMeetings(today);
        List<TodayMeetingDTO> todayMeetingList = statisticsMapper.getTodayMeetingList(today);

        // 可选：获取今日待审核和今日已通过的会议数量
        Integer todayPendingApplications = statisticsMapper.countTodayPendingApplications(today);
        Integer todayApprovedMeetings = statisticsMapper.countTodayApprovedMeetings(today);

        // 构建返回对象
        AdminStatisticsDTO statistics = new AdminStatisticsDTO();
        statistics.setTotalMeetingRooms(totalMeetingRooms);
        statistics.setPendingApplications(pendingApplications);
        statistics.setReviewedApplications(reviewedApplications);
        statistics.setTodayMeetings(todayMeetings);
        statistics.setTodayMeetingList(todayMeetingList);
        statistics.setTodayPendingApplications(todayPendingApplications);
        statistics.setTodayApprovedMeetings(todayApprovedMeetings);

        return statistics;
    }


    @Override
    public RoomFrequencyChartDTO getRoomFrequencyChart(FrequencyQueryDTO query) {
        // 设置默认值
        if (query.getPeriod() == null) {
            query.setPeriod("week");
        }
        if (query.getSortBy() == null) {
            query.setSortBy("count_desc");
        }
        if (query.getIncludeZeroUsage() == null) {
            query.setIncludeZeroUsage(true);
        }

        // 获取排名数据
        List<RoomUsageRankDTO> rankList = statisticsMapper.getRoomUsageRank(query);

        // 构建图表数据
        RoomFrequencyChartDTO chartData = statisticsMapper.getRoomFrequencyChart(query);

        if (chartData != null && rankList != null && !rankList.isEmpty()) {
            // 提取会议室名称列表
            List<String> roomNames = rankList.stream()
                    .map(RoomUsageRankDTO::getRoomName)
                    .collect(Collectors.toList());

            // 提取使用次数列表
            List<Integer> usageCounts = rankList.stream()
                    .map(RoomUsageRankDTO::getUsageCount)
                    .collect(Collectors.toList());

            // 设置图表数据
            chartData.setRoomNames(roomNames);
            chartData.setUsageCounts(usageCounts);

            // 设置统计时间
            chartData.setStatTime(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }

        return chartData;
    }

    @Override
    public List<RoomUsageRankDTO> getRoomUsageRank(FrequencyQueryDTO query) {
        // 设置默认值
        if (query.getPeriod() == null) {
            query.setPeriod("week");
        }
        if (query.getSortBy() == null) {
            query.setSortBy("count_desc");
        }
        if (query.getIncludeZeroUsage() == null) {
            query.setIncludeZeroUsage(true);
        }

        // 获取排名数据
        List<RoomUsageRankDTO> rankList = statisticsMapper.getRoomUsageRank(query);

        // 如果需要对比上周数据，则获取上周使用次数
        if (rankList != null && !rankList.isEmpty()) {
            // 获取当前周期的开始和结束日期
            Map<String, LocalDate> periodRange = statisticsMapper.getPeriodDateRange(query.getPeriod());

            // 提取会议室ID列表
            List<Integer> roomIds = rankList.stream()
                    .map(RoomUsageRankDTO::getRoomId)
                    .collect(Collectors.toList());

            // 获取上周使用次数
            Map<Integer, Integer> lastWeekUsage = statisticsMapper.getLastWeekUsageCount(
                    roomIds,
                    periodRange.get("start_date"),
                    periodRange.get("end_date")
            );

            // 计算变化百分比
            for (RoomUsageRankDTO rankDTO : rankList) {
                Integer currentUsage = rankDTO.getUsageCount();
                Integer lastWeekUsageCount = lastWeekUsage.getOrDefault(rankDTO.getRoomId(), 0);
                rankDTO.setLastWeekUsageCount(lastWeekUsageCount);

                // 计算变化百分比
                if (lastWeekUsageCount > 0) {
                    double changePercentage = ((currentUsage - lastWeekUsageCount) * 100.0) / lastWeekUsageCount;
                    rankDTO.setChangePercentage(Math.round(changePercentage * 100.0) / 100.0);
                } else if (currentUsage > 0) {
                    rankDTO.setChangePercentage(100.0); // 从0到有使用，增长100%
                } else {
                    rankDTO.setChangePercentage(0.0);
                }
            }
        }

        return rankList;
    }

    @Override
    public Map<String, Object> getRoomFrequencyDashboard(FrequencyQueryDTO query) {
        Map<String, Object> dashboard = new HashMap<>();

        // 1. 获取频率图表数据
        RoomFrequencyChartDTO chartData = getRoomFrequencyChart(query);
        dashboard.put("chartData", chartData);

        // 2. 获取排名数据
        List<RoomUsageRankDTO> rankData = getRoomUsageRank(query);
        dashboard.put("rankData", rankData);

        // 3. 获取统计摘要
        Map<String, Object> summary = new HashMap<>();
        if (chartData != null) {
            summary.put("totalRooms", chartData.getRoomNames() != null ? chartData.getRoomNames().size() : 0);
            summary.put("totalUsage", chartData.getTotalUsageCount());
            summary.put("averageUsage", chartData.getAverageUsageCount());
            summary.put("maxUsage", chartData.getMaxUsageCount());
            summary.put("minUsage", chartData.getMinUsageCount());
            summary.put("statPeriod", chartData.getPeriod());
            summary.put("statRange", chartData.getStartDate() + " ~ " + chartData.getEndDate());
        }
        dashboard.put("summary", summary);

        // 4. 获取热门会议室（使用次数最多的前3个）
        List<Map<String, Object>> topRooms = new ArrayList<>();
        if (rankData != null && !rankData.isEmpty()) {
            rankData.stream()
                    .limit(3)
                    .forEach(room -> {
                        Map<String, Object> roomInfo = new HashMap<>();
                        roomInfo.put("roomName", room.getRoomName());
                        roomInfo.put("usageCount", room.getUsageCount());
                        roomInfo.put("usageRate", room.getUsageRate());
                        roomInfo.put("changePercentage", room.getChangePercentage());
                        topRooms.add(roomInfo);
                    });
        }
        dashboard.put("topRooms", topRooms);

        return dashboard;
    }

    @Override
    public Map<String, Object> getRoomFrequencyComparison(FrequencyQueryDTO query1, FrequencyQueryDTO query2) {
        Map<String, Object> comparison = new HashMap<>();

        // 获取第一个时间段的数据
        List<RoomUsageRankDTO> data1 = getRoomUsageRank(query1);
        // 获取第二个时间段的数据
        List<RoomUsageRankDTO> data2 = getRoomUsageRank(query2);

        // 构建对比数据
        List<Map<String, Object>> comparisonList = new ArrayList<>();

        // 使用会议室ID作为键，方便查找
        Map<Integer, RoomUsageRankDTO> map1 = data1.stream()
                .collect(Collectors.toMap(RoomUsageRankDTO::getRoomId, dto -> dto));

        Map<Integer, RoomUsageRankDTO> map2 = data2.stream()
                .collect(Collectors.toMap(RoomUsageRankDTO::getRoomId, dto -> dto));

        // 合并两个时间段的数据
        Set<Integer> allRoomIds = new HashSet<>();
        allRoomIds.addAll(map1.keySet());
        allRoomIds.addAll(map2.keySet());

        for (Integer roomId : allRoomIds) {
            RoomUsageRankDTO dto1 = map1.get(roomId);
            RoomUsageRankDTO dto2 = map2.get(roomId);

            Map<String, Object> comp = new HashMap<>();
            comp.put("roomId", roomId);
            comp.put("roomName", dto1 != null ? dto1.getRoomName() :
                    dto2 != null ? dto2.getRoomName() : "未知");

            int count1 = dto1 != null ? dto1.getUsageCount() : 0;
            int count2 = dto2 != null ? dto2.getUsageCount() : 0;

            comp.put("usageCount1", count1);
            comp.put("usageCount2", count2);
            comp.put("usageChange", count2 - count1);

            // 计算变化百分比
            if (count1 > 0) {
                double changePercentage = ((count2 - count1) * 100.0) / count1;
                comp.put("changePercentage", Math.round(changePercentage * 100.0) / 100.0);
            } else if (count2 > 0) {
                comp.put("changePercentage", 100.0);
            } else {
                comp.put("changePercentage", 0.0);
            }

            comparisonList.add(comp);
        }

        // 按变化量排序
        comparisonList.sort((a, b) -> {
            Integer changeA = (Integer) a.get("usageChange");
            Integer changeB = (Integer) b.get("usageChange");
            return changeB.compareTo(changeA); // 降序
        });

        comparison.put("period1", query1.getPeriod());
        comparison.put("period2", query2.getPeriod());
        comparison.put("comparisonData", comparisonList);
        comparison.put("totalRooms", allRoomIds.size());

        return comparison;
    }
}