package com.uestcfir.service;

import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class BusinessCacheInvalidator {
    private final CacheManager manager;

    public BusinessCacheInvalidator(CacheManager manager) { this.manager = manager; }

    public void roomsChanged() {
        afterCommit(() -> manager.getCache("meetingroom:list").clear());
    }

    public void roomDeleted(Integer roomId) {
        roomsChanged();
        safeEvict("iot:latest-room", roomId);
        safeEvict("iot:room-status", roomId);
        afterCommit(() -> manager.getCache("iot:recent-room").clear());
    }

    public void telemetryChanged(Integer roomId, String deviceId) {
        safeEvict("iot:latest-room", roomId);
        safeEvict("iot:room-status", roomId);
        safeEvict("iot:latest-device", deviceId);
        afterCommit(() -> manager.getCache("iot:recent-room").clear());
    }

    private void safeEvict(String cacheName, Object key) {
        afterCommit(() -> manager.getCache(cacheName).evict(key));
    }

    private void afterCommit(Runnable action) {
        Runnable bestEffort = () -> {
            try { action.run(); }
            catch (RuntimeException exception) {
                // Database writes have committed; cache availability must not change their result.
                LoggerFactory.getLogger(BusinessCacheInvalidator.class).warn(
                        "Business cache invalidation failed ({}); entries will expire by TTL",
                        exception.getClass().getSimpleName());
            }
        };
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { bestEffort.run(); }
            });
        } else {
            bestEffort.run();
        }
    }
}
