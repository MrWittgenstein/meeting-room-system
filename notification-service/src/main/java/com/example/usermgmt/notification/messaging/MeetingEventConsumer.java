package com.example.usermgmt.notification.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.usermgmt.notification.dto.SiteMessageSendRequest;
import com.example.usermgmt.notification.service.SiteMessageService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.core.Message;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "meeting.events", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MeetingEventConsumer {

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final SiteMessageService siteMessageService;
    private final TransactionTemplate transactionTemplate;

    @RabbitListener(queues = "${meeting.events.queue:meeting.notification.events}", containerFactory = "meetingEventListenerContainerFactory")
    public void consume(Message message) {
        MeetingEvent event;
        try {
            event = objectMapper.readValue(message.getBody(), MeetingEvent.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("会议事件格式无效", e);
        }
        try {
            transactionTemplate.executeWithoutResult(status -> processInTransaction(event));
        } catch (Exception e) {
            log.error("会议事件消费失败 eventId={}, type={}", event.eventId(), event.eventType(), e);
            throw e;
        }
    }

    private void processInTransaction(MeetingEvent event) {
        int inserted = jdbcTemplate.update(
                "INSERT IGNORE INTO consumed_meeting_event (event_id, event_type, status, attempts) VALUES (?, ?, 'PROCESSING', 1)",
                event.eventId(), event.eventType()
        );
        if (inserted == 0) {
            Integer processed = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM consumed_meeting_event WHERE event_id = ? AND status = 'PROCESSED'",
                    Integer.class,
                    event.eventId()
            );
            if (processed != null && processed > 0) {
                return;
            }
            jdbcTemplate.update(
                    "UPDATE consumed_meeting_event SET status = 'PROCESSING', attempts = attempts + 1, last_error = NULL WHERE event_id = ?",
                    event.eventId()
            );
        }

        SiteMessageSendRequest request = toSiteMessage(event);
        if (request != null) {
            siteMessageService.send(request);
        }
        jdbcTemplate.update("UPDATE consumed_meeting_event SET status = 'PROCESSED', processed_time = NOW(), last_error = NULL WHERE event_id = ?", event.eventId());
    }

    private SiteMessageSendRequest toSiteMessage(MeetingEvent event) {
        Map<String, Object> payload = event.payload();
        List<String> userIds = stringList(payload.get("userIds"));
        if (userIds.isEmpty()) {
            return null;
        }
        String eventType = event.eventType();
        if (!eventType.startsWith("announcement.") && !eventType.startsWith("reservation.")) {
            return null;
        }
        SiteMessageSendRequest request = new SiteMessageSendRequest();
        request.setUserIds(userIds);
        request.setTitle(text(payload.get("title"), titleFor(eventType)));
        request.setContent(text(payload.get("content"), contentFor(eventType, payload)));
        request.setCategory(text(payload.get("category"), eventType));
        request.setLink(text(payload.get("link"), ""));
        request.setCreatorId(text(payload.get("creatorId"), "system"));
        request.setDeferDispatch(true);
        try {
            request.setParams(payload.get("params") == null ? null : objectMapper.valueToTree(payload.get("params")));
        } catch (IllegalArgumentException ignored) {
            // Params are optional; notification content is still delivered.
        }
        return request;
    }

    private String titleFor(String eventType) {
        return switch (eventType) {
            case "reservation.approved" -> "预约已通过";
            case "reservation.rejected" -> "预约未通过";
            case "reservation.cancelled" -> "预约已取消";
            case "reservation.expired" -> "预约已过期";
            case "reservation.created" -> "预约已提交";
            case "reservation.reminder" -> "会议提醒";
            default -> "系统通知";
        };
    }

    private String contentFor(String eventType, Map<String, Object> payload) {
        String room = text(payload.get("roomName"), "会议室");
        return switch (eventType) {
            case "reservation.approved" -> room + "预约已审批通过";
            case "reservation.rejected" -> room + "预约未通过审批";
            case "reservation.cancelled" -> room + "预约已取消";
            case "reservation.expired" -> room + "预约已过期";
            case "reservation.created" -> room + "预约申请已提交";
            case "reservation.reminder" -> "您预约的会议即将在15分钟内开始";
            default -> "您有一条新的系统通知";
        };
    }

    private List<String> stringList(Object value) {
        if (value instanceof List<?> values) {
            return values.stream().map(String::valueOf).filter(valueText -> !valueText.isBlank()).toList();
        }
        return value == null ? List.of() : List.of(String.valueOf(value));
    }

    private String text(Object value, String fallback) {
        String text = value == null ? "" : String.valueOf(value);
        return text.isBlank() ? fallback : text;
    }

}
