package com.uestcfir.service;

import com.uestcfir.mapper.ImportantNewsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ImportantNewsScheduleService {

    @Autowired
    private ImportantNewsMapper importantNewsMapper;

    /**
     * 自动更新重要信息过期状态 - 项目启动后立即执行，然后每1分钟执行一次
     */
    @Scheduled(initialDelay = 5000, fixedRate = 60000)
    public void autoUpdateNewsExpiredStatus() {
        log.info("开始自动更新重要信息过期状态...");

        try {
            int updatedCount = importantNewsMapper.updateExpiredNewsStatus();
            log.info("重要信息过期状态更新完成，更新了 {} 条记录", updatedCount);

        } catch (Exception e) {
            log.error("自动更新重要信息过期状态失败: {}", e.getMessage());
        }
    }
}
