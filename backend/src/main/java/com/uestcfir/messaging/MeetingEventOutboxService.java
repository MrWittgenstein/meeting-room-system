package com.uestcfir.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/** Stores events with the business transaction and publishes them asynchronously. */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingEventOutboxService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final MeetingEventProperties properties;
    private final RabbitTemplate rabbitTemplate;

    public boolean enqueue(String eventType, Map<String, Object> payload) {
        if (!properties.isEnabled()) {
            return false;
        }

        String eventId = UUID.randomUUID().toString();
        MeetingEvent event = new MeetingEvent(eventId, eventType, Instant.now(), payload);
        try {
            jdbcTemplate.update(
                    """
                    INSERT INTO meeting_event_outbox
                        (event_id, event_type, routing_key, payload, status, attempts, next_attempt_at)
                    VALUES (?, ?, ?, CAST(? AS JSON), 'PENDING', 0, NOW())
                    """,
                    event.eventId(),
                    event.eventType(),
                    event.eventType(),
                    objectMapper.writeValueAsString(event)
            );
            return true;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("无法序列化会议事件: " + eventType, e);
        }
    }

    public void dispatchPending() {
        if (!properties.isEnabled()) {
            return;
        }

        List<OutboxRecord> records = jdbcTemplate.query(
                """
                SELECT event_id, event_type, payload
                FROM meeting_event_outbox
                WHERE (status IN ('PENDING', 'FAILED') AND next_attempt_at <= NOW())
                   OR (status = 'PUBLISHING' AND update_time < DATE_SUB(NOW(), INTERVAL 5 MINUTE))
                ORDER BY created_time
                LIMIT 100
                """,
                (rs, rowNum) -> new OutboxRecord(
                        rs.getString("event_id"),
                        rs.getString("event_type"),
                        rs.getString("payload")
                )
        );

        for (OutboxRecord record : records) {
            int claimed = jdbcTemplate.update(
                    """
                    UPDATE meeting_event_outbox
                    SET status = 'PUBLISHING', attempts = attempts + 1, update_time = NOW()
                    WHERE event_id = ?
                      AND (status IN ('PENDING', 'FAILED')
                           OR (status = 'PUBLISHING' AND update_time < DATE_SUB(NOW(), INTERVAL 5 MINUTE)))
                    """,
                    record.eventId()
            );
            if (claimed == 0) {
                continue;
            }

            try {
                // The payload is already JSON. Keeping it as text avoids type metadata coupling between services.
                publish(record);
                jdbcTemplate.update(
                        """
                        UPDATE meeting_event_outbox
                        SET status = 'PUBLISHED', published_time = NOW(), update_time = NOW(), last_error = NULL
                        WHERE event_id = ? AND status = 'PUBLISHING'
                        """,
                        record.eventId()
                );
            } catch (Exception e) {
                String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                log.error("会议事件发布失败 eventId={}, type={}: {}", record.eventId(), record.eventType(), message);
                jdbcTemplate.update(
                        """
                        UPDATE meeting_event_outbox
                        SET status = 'FAILED', next_attempt_at = DATE_ADD(NOW(), INTERVAL LEAST(600, POW(2, LEAST(attempts, 9))) SECOND),
                            last_error = LEFT(?, 1000), update_time = NOW()
                        WHERE event_id = ? AND status = 'PUBLISHING'
                        """,
                        message,
                        record.eventId()
                );
            }
        }
    }

    private void publish(OutboxRecord record) {
        CorrelationData correlationData = new CorrelationData(record.eventId());
        rabbitTemplate.convertAndSend(properties.getExchange(), record.eventType(), record.payload(), correlationData);
        CorrelationData.Confirm confirm;
        try {
            confirm = correlationData.getFuture().get(Duration.ofSeconds(5).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("RabbitMQ 未确认事件发布", e);
        }
        if (!confirm.isAck()) {
            throw new IllegalStateException("RabbitMQ 拒绝事件: " + confirm.getReason());
        }
        if (correlationData.getReturned() != null) {
            throw new IllegalStateException("事件没有路由到队列: " + correlationData.getReturned().getReplyText());
        }
    }

    public record OutboxRecord(String eventId, String eventType, String payload) {
    }
}
