package com.uestcfir.cacheinvalidation;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CanalCacheInvalidationRunner implements CommandLineRunner {
    private final CacheInvalidationProperties properties;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "canal-cache-invalidation");
        thread.setDaemon(true);
        return thread;
    });
    private volatile CanalConnector connector;

    @Override
    public void run(String... args) {
        objectMapper.findAndRegisterModules();
        executor.submit(this::consumeLoop);
    }

    private void consumeLoop() {
        while (running.get()) {
            try {
                connector = CanalConnectors.newSingleConnector(
                        new InetSocketAddress(properties.getCanalHost(), properties.getCanalPort()),
                        properties.getCanalDestination(), properties.getCanalUsername(), properties.getCanalPassword());
                connector.connect();
                connector.subscribe("meetingroom\\..*");
                connector.rollback();
                log.info("Connected to Canal {}:{}, destination={}", properties.getCanalHost(), properties.getCanalPort(), properties.getCanalDestination());
                consumeConnected();
            } catch (Exception e) {
                log.warn("Canal consumer disconnected: {}", e.getMessage());
                closeConnector();
                sleepBeforeReconnect();
            }
        }
    }

    private void consumeConnected() throws Exception {
        while (running.get()) {
            Message message = connector.getWithoutAck(properties.getBatchSize());
            long batchId = message.getId();
            if (batchId == -1L || message.getEntries().isEmpty()) {
                sleep(500L);
                continue;
            }
            try {
                for (CanalEntry.Entry entry : message.getEntries()) {
                    publishInvalidations(entry, batchId);
                }
                connector.ack(batchId);
            } catch (Exception e) {
                connector.rollback(batchId);
                throw e;
            }
        }
    }

    private void publishInvalidations(CanalEntry.Entry entry, long batchId) throws Exception {
        if (entry.getEntryType() != CanalEntry.EntryType.ROWDATA) return;
        CanalEntry.RowChange change = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
        String table = entry.getHeader().getTableName();
        int rowIndex = 0;
        for (CanalEntry.RowData row : change.getRowDatasList()) {
            Map<String, String> columns = columns(change.getEventType() == CanalEntry.EventType.DELETE
                    ? row.getBeforeColumnsList() : row.getAfterColumnsList());
            for (CacheInvalidation invalidation : resolve(table, columns)) {
                String eventId = "canal:" + batchId + ":" + entry.getHeader().getLogfileOffset() + ":" + rowIndex + ":" + invalidation.cacheName() + ":" + invalidation.key();
                Map<String, Object> event = new LinkedHashMap<>();
                event.put("eventId", eventId);
                event.put("cacheName", invalidation.cacheName());
                event.put("key", invalidation.key());
                event.put("clear", invalidation.clear());
                event.put("source", "canal");
                event.put("occurredAt", Instant.now());
                rabbitTemplate.convertAndSend(properties.getRabbitExchange(), properties.getRabbitRoutingKey(), objectMapper.writeValueAsString(event));
            }
            rowIndex++;
        }
    }

    private List<CacheInvalidation> resolve(String table, Map<String, String> columns) {
        List<CacheInvalidation> invalidations = new ArrayList<>();
        if ("meetingroom".equalsIgnoreCase(table)) {
            invalidations.add(new CacheInvalidation("meetingroom:list", "", true));
            return invalidations;
        }
        if (SetOfTables.IOT.contains(table.toLowerCase())) {
            String roomId = columns.get("room_id");
            String deviceId = columns.get("device_id");
            if (roomId != null && !roomId.isBlank()) {
                invalidations.add(new CacheInvalidation("iot:latest-room", roomId, false));
                invalidations.add(new CacheInvalidation("iot:room-status", roomId, false));
            }
            if (deviceId != null && !deviceId.isBlank()) {
                invalidations.add(new CacheInvalidation("iot:latest-device", deviceId, false));
            }
            invalidations.add(new CacheInvalidation("iot:recent-room", "", true));
        }
        return invalidations;
    }

    private Map<String, String> columns(List<CanalEntry.Column> columns) {
        Map<String, String> result = new LinkedHashMap<>();
        for (CanalEntry.Column column : columns) {
            result.put(column.getName().toLowerCase(), column.getValue());
        }
        return result;
    }

    private void closeConnector() {
        if (connector != null) {
            try { connector.disconnect(); } catch (Exception ignored) { }
            connector = null;
        }
    }

    private void sleepBeforeReconnect() { sleep(5000L); }
    private void sleep(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private record CacheInvalidation(String cacheName, String key, boolean clear) { }

    private static final class SetOfTables {
        private static final java.util.Set<String> IOT = java.util.Set.of("iot_sensor_record", "iot_room_status", "iot_device");
    }
}
