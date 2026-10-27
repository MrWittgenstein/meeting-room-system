package com.uestcfir.service;

import com.uestcfir.mapper.ImportantNewsMapper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ImportantNewsScheduleService {

    @Autowired
    private ImportantNewsMapper importantNewsMapper;

    /**
     * 由 XXL-JOB 调度，更新重要信息过期状态。
     */
    @XxlJob("importantNewsExpiryUpdate")
    public void autoUpdateNewsExpiredStatus() {
        log.info("开始自动更新重要信息过期状态...");

        try {
            int updatedCount = importantNewsMapper.updateExpiredNewsStatus();
            log.info("重要信息过期状态更新完成，更新了 {} 条记录", updatedCount);

        } catch (Exception e) {
            log.error("自动更新重要信息过期状态失败: {}", e.getMessage(), e);
            throw new IllegalStateException("更新重要信息过期状态失败", e);
        }
    }
}
