package com.uestcfir.mapper;

import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface MeetingRoomStatisticsMapper {

    // 会议室总数
    @Select("SELECT COUNT(*) FROM meetingroom WHERE status = 0")
    Integer getTotalRooms();

    // 已审核申请数
    @Select("SELECT COUNT(*) FROM reservations WHERE status IN (1, 4, 5)") // 假设1:通过,2:拒绝,3:已完成
    Integer getReviewedApplications();

    // 待审核申请数
    @Select("SELECT COUNT(*) FROM reservations WHERE status = 0") // 假设0:待审核
    Integer getPendingApplications();

    // 今日审批通过会议数（按类型）
    @Select({
            "<script>",
            "SELECT mr.type, COUNT(*) as count",
            "FROM reservations r",
            "INNER JOIN meetingroom mr ON r.room_id = mr.room_id",
            "WHERE r.status = 1", // 审批通过
            "AND DATE(r.reserve_date) = CURDATE()",
            "GROUP BY mr.type",
            "</script>"
    })
    @MapKey("type")
    Map<String, Integer> getTodayApprovedByType();

    // 本周审批通过会议数（按类型）
    @Select({
            "<script>",
            "SELECT mr.type, COUNT(*) as count",
            "FROM reservations r",
            "INNER JOIN meetingroom mr ON r.room_id = mr.room_id",
            "WHERE r.status = 1",
            "AND YEARWEEK(r.reserve_date) = YEARWEEK(CURDATE())",
            "GROUP BY mr.type",
            "</script>"
    })
    @MapKey("type")
    Map<String, Integer> getWeekApprovedByType();

    // 本月审批通过会议数（按类型）
    @Select({
            "<script>",
            "SELECT mr.type, COUNT(*) as count",
            "FROM reservations r",
            "INNER JOIN meetingroom mr ON r.room_id = mr.room_id",
            "WHERE r.status = 1",
            "AND YEAR(r.reserve_date) = YEAR(CURDATE())",
            "AND MONTH(r.reserve_date) = MONTH(CURDATE())",
            "GROUP BY mr.type",
            "</script>"
    })
    @MapKey("type")
    Map<String, Integer> getMonthApprovedByType();

    // 今日会议室申请数（按类型）
    @Select({
            "<script>",
            "SELECT mr.type, COUNT(*) as count",
            "FROM reservations r",
            "INNER JOIN meetingroom mr ON r.room_id = mr.room_id",
            "WHERE DATE(r.created_time) = CURDATE()",
            "GROUP BY mr.type",
            "</script>"
    })
    @MapKey("type")
    Map<String, Integer> getTodayApplicationsByType();

    // 本周会议室申请数（按类型）
    @Select({
            "<script>",
            "SELECT mr.type, COUNT(*) as count",
            "FROM reservations r",
            "INNER JOIN meetingroom mr ON r.room_id = mr.room_id",
            "WHERE YEARWEEK(r.created_time) = YEARWEEK(CURDATE())",
            "GROUP BY mr.type",
            "</script>"
    })
    @MapKey("type")
    Map<String, Integer> getWeekApplicationsByType();

    // 本月会议室申请数（按类型）
    @Select({
            "<script>",
            "SELECT mr.type, COUNT(*) as count",
            "FROM reservations r",
            "INNER JOIN meetingroom mr ON r.room_id = mr.room_id",
            "WHERE YEAR(r.created_time) = YEAR(CURDATE())",
            "AND MONTH(r.created_time) = MONTH(CURDATE())",
            "GROUP BY mr.type",
            "</script>"
    })
    @MapKey("type")
    Map<String, Integer> getMonthApplicationsByType();
}