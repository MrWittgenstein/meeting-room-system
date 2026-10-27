package com.example.usermgmt.notification.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.usermgmt.notification.service.SiteMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class MeetingEventConsumerTest {

    private JdbcTemplate jdbcTemplate;
    private SiteMessageService siteMessageService;
    private MeetingEventConsumer consumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:meeting-events;MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("DROP TABLE IF EXISTS consumed_meeting_event");
        jdbcTemplate.execute("""
                CREATE TABLE consumed_meeting_event (
                    event_id VARCHAR(64) PRIMARY KEY,
                    event_type VARCHAR(100) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    attempts INT NOT NULL,
                    last_error VARCHAR(1000),
                    processed_time TIMESTAMP,
                    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        siteMessageService = org.mockito.Mockito.mock(SiteMessageService.class);
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        consumer = new MeetingEventConsumer(
                objectMapper,
                jdbcTemplate,
                siteMessageService,
                new TransactionTemplate(new DataSourceTransactionManager(dataSource))
        );
    }

    @Test
    void duplicateDeliveryCreatesTheSiteMessageOnlyOnce() throws Exception {
        doReturn(1).when(siteMessageService).send(any());
        Message message = message("event-1");

        consumer.consume(message);
        consumer.consume(message);

        verify(siteMessageService, times(1)).send(any());
        assertEquals("PROCESSED", jdbcTemplate.queryForObject(
                "SELECT status FROM consumed_meeting_event WHERE event_id = 'event-1'",
                String.class
        ));
    }

    @Test
    void failedDeliveryRollsBackTheIdempotencyRecordForRetry() throws Exception {
        when(siteMessageService.send(any()))
                .thenThrow(new IllegalStateException("temporary failure"))
                .thenReturn(1);
        Message message = message("event-2");

        assertThrows(IllegalStateException.class, () -> consumer.consume(message));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM consumed_meeting_event", Integer.class));

        consumer.consume(message);

        verify(siteMessageService, times(2)).send(any());
        assertEquals("PROCESSED", jdbcTemplate.queryForObject(
                "SELECT status FROM consumed_meeting_event WHERE event_id = 'event-2'",
                String.class
        ));
    }

    private Message message(String eventId) throws Exception {
        MeetingEvent event = new MeetingEvent(
                eventId,
                "reservation.approved",
                Instant.parse("2026-10-12T00:00:00Z"),
                Map.of("userIds", List.of("user-1"), "roomName", "A101")
        );
        return new Message(objectMapper.writeValueAsBytes(event), new MessageProperties());
    }
}
