package com.uestcfir.controller.approver;


import com.uestcfir.auth.CurrentUserContext;
import com.uestcfir.auth.RolePermissionRegistry;
import com.uestcfir.pojo.dto.AdminStatisticsDTO;
import com.uestcfir.pojo.dto.FrequencyQueryDTO;
import com.uestcfir.pojo.dto.RoomFrequencyChartDTO;
import com.uestcfir.pojo.dto.RoomUsageRankDTO;
import com.uestcfir.pojo.entity.Result;
import com.uestcfir.service.StatisticsService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor

public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/admin")
    public Result getAdminStatistics() {
        CurrentUserContext.requirePermission(RolePermissionRegistry.STATISTICS_READ);
        AdminStatisticsDTO statistics = statisticsService.getAdminStatistics();
        return Result.success(statistics);
    }

    @GetMapping("/room-frequency/chart")
    public Result getRoomFrequencyChart(
            @RequestParam(defaultValue = "week") String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "count_desc") String sortBy,
            @RequestParam(required = false) Integer limit,
            @RequestParam(defaultValue = "true") Boolean includeZeroUsage,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) String location) {

        CurrentUserContext.requirePermission(RolePermissionRegistry.STATISTICS_READ);
        FrequencyQueryDTO query = new FrequencyQueryDTO();
        query.setPeriod(period);
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        query.setSortBy(sortBy);
        query.setLimit(limit);
        query.setIncludeZeroUsage(includeZeroUsage);
        query.setRoomType(roomType);
        query.setLocation(location);

        RoomFrequencyChartDTO result = statisticsService.getRoomFrequencyChart(query);
        return Result.success(result);
    }

    @GetMapping("/room-frequency/rank")
    public Result getRoomUsageRank(
            @RequestParam(defaultValue = "week") String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "count_desc") String sortBy,
            @RequestParam(required = false) Integer limit,
            @RequestParam(defaultValue = "true") Boolean includeZeroUsage,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) String location) {

        CurrentUserContext.requirePermission(RolePermissionRegistry.STATISTICS_READ);
        FrequencyQueryDTO query = new FrequencyQueryDTO();
        query.setPeriod(period);
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        query.setSortBy(sortBy);
        query.setLimit(limit);
        query.setIncludeZeroUsage(includeZeroUsage);
        query.setRoomType(roomType);
        query.setLocation(location);

        List<RoomUsageRankDTO> result = statisticsService.getRoomUsageRank(query);
        return Result.success(result);
    }

    @GetMapping("/room-frequency/dashboard")
    public Result getRoomFrequencyDashboard(
            @RequestParam(defaultValue = "week") String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "count_desc") String sortBy,
            @RequestParam(required = false) Integer limit,
            @RequestParam(defaultValue = "true") Boolean includeZeroUsage,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) String location) {

        CurrentUserContext.requirePermission(RolePermissionRegistry.STATISTICS_READ);
        FrequencyQueryDTO query = new FrequencyQueryDTO();
        query.setPeriod(period);
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        query.setSortBy(sortBy);
        query.setLimit(limit);
        query.setIncludeZeroUsage(includeZeroUsage);
        query.setRoomType(roomType);
        query.setLocation(location);

        Map<String, Object> result = statisticsService.getRoomFrequencyDashboard(query);
        return Result.success(result);
    }

    @GetMapping("/room-frequency/comparison")
    public Result getRoomFrequencyComparison(
            @RequestParam(defaultValue = "week") String period1,
            @RequestParam(required = false) String startDate1,
            @RequestParam(required = false) String endDate1,
            @RequestParam(defaultValue = "week") String period2,
            @RequestParam(required = false) String startDate2,
            @RequestParam(required = false) String endDate2) {

        CurrentUserContext.requirePermission(RolePermissionRegistry.STATISTICS_READ);
        FrequencyQueryDTO query1 = new FrequencyQueryDTO();
        query1.setPeriod(period1);
        query1.setStartDate(startDate1);
        query1.setEndDate(endDate1);

        FrequencyQueryDTO query2 = new FrequencyQueryDTO();
        query2.setPeriod(period2);
        query2.setStartDate(startDate2);
        query2.setEndDate(endDate2);

        Map<String, Object> result = statisticsService.getRoomFrequencyComparison(query1, query2);
        return Result.success(result);
    }

    @GetMapping("/room-frequency/weekly")
    public Result getWeeklyRoomFrequency() {
        CurrentUserContext.requirePermission(RolePermissionRegistry.STATISTICS_READ);
        FrequencyQueryDTO query = new FrequencyQueryDTO();
        query.setPeriod("week");
        query.setSortBy("count_desc");
        query.setIncludeZeroUsage(true);

        RoomFrequencyChartDTO result = statisticsService.getRoomFrequencyChart(query);
        return Result.success(result);
    }
}
