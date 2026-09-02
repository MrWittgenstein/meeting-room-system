package com.uestcfir.service;

import com.uestcfir.mapper.ReservationMapper;
import com.uestcfir.pojo.entity.Reservations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ReservationStateScheduleService {

    @Autowired
    private ReservationMapper reservationMapper;

    /**
     * 自动更新预约状态 - 项目启动后立即执行，然后每1分钟执行一次
     */
    @Scheduled(initialDelay = 5000, fixedRate = 60000)
    public void autoUpdateReservationStates() {
        log.info("开始自动更新预约状态...");

        try {

            // 任务1：处理status=2（已过期）的预约，更新state为"已过期"
            int expiredUpdated = updateExpiredReservations();


            List<Reservations> reservations = reservationMapper.selectApprovedReservations();
            int updatedCount = 0;

            for (Reservations reservation : reservations) {
                String newState = calculateState(reservation);
                reservationMapper.updateReservationState(reservation.getReservationId(), newState);
                updatedCount++;
            }

            log.info("自动更新完成，更新了 {} 条过期预约状态，更新了 {} 条记录", expiredUpdated,updatedCount);

        } catch (Exception e) {
            log.error("自动更新预约状态失败: {}", e.getMessage());
        }
    }


    /**
     * 处理已过期的预约（status=2），将state更新为"已过期"
     */
    private int updateExpiredReservations() {
        // 查询所有status=2但state不为"已过期"的预约
        List<Reservations> expiredReservations = reservationMapper.selectExpiredReservations();

        int updatedCount = 0;
        for (Reservations reservation : expiredReservations) {
            // 如果state不是"已过期"，则更新为"已过期"
            if (!"已过期".equals(reservation.getState())) {
                reservationMapper.updateReservationState(reservation.getReservationId(), "已过期");
                updatedCount++;
            }
        }

        return updatedCount;
    }

    /**
     * 计算状态
     */
    private String calculateState(Reservations reservation) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDateTime = LocalDateTime.of(reservation.getReserveDate(), reservation.getStartTime());
        LocalDateTime endDateTime = LocalDateTime.of(reservation.getReserveDate(), reservation.getEndTime());

        if (endDateTime.isBefore(now)) {
            return "已结束";
        } else if (!startDateTime.isAfter(now) && !endDateTime.isBefore(now)) {
            return "正在使用";
        } else {
            Duration duration = Duration.between(now, startDateTime);
            return formatRemainingTime(duration);
        }
    }

    /**
     * 格式化剩余时间
     */
    private String formatRemainingTime(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;

        if (days > 0) {
            return "距离开始还有" + days + "天" + hours + "小时";
        } else if (hours > 0) {
            return "距离开始还有" + hours + "小时" + minutes + "分钟";
        } else {
            return "距离开始还有" + minutes + "分钟";
        }
    }
}
