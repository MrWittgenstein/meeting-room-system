package com.uestcfir.task;


import com.uestcfir.mapper.ReservationMapper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
@Slf4j
public class ReservationExpiryTask {

    @Autowired
    private ReservationMapper reservationMapper;

    @XxlJob("reservationExpiryCheck")
    public void checkReservationExpiry() {
        log.info("开始检查预约是否过期");
        try {
            LocalTime now = LocalTime.now();
            LocalDate today = LocalDate.now();
             int updatedCount = reservationMapper.updateExpiredReservations(now, today);

            if (updatedCount > 0) {
            log.info("成功标记 {} 个过期预约", updatedCount);
         }
    } catch (Exception e) {
        log.error("检查过期预约时发生错误", e);
        throw new IllegalStateException("检查过期预约失败", e);
    }

    }

    @XxlJob("expiredReservationCleanup")
    public void cleanupExpiredReservations() {
        try {
            LocalDateTime deadline = LocalDateTime.now().minusDays(10);//10天前的日期,保留十天内的预约
            int deletedCount = reservationMapper.deleteExpiredReservations(deadline);
            if (deletedCount > 0) {
                log.info("成功删除 {} 个过期预约", deletedCount);
            }
        } catch (Exception e) {
            log.error("清理过期预约时发生错误", e);
            throw new IllegalStateException("清理过期预约失败", e);
        }
    }
}
