package com.uestcfir.client;

import com.uestcfir.config.PlatformServiceProperties;
import com.uestcfir.messaging.MeetingEventPublisher;
import com.uestcfir.pojo.entity.Reservations;
import com.uestcfir.pojo.entity.ImportantNews;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Adapter for notification-service. The meeting domain never owns delivery credentials. */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationServiceClient {
    private final PlatformServiceProperties properties;
    private final UserServiceClient userServiceClient;
    private final MeetingEventPublisher meetingEventPublisher;

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public boolean sendMeetingReminder(Reservations reservation) {
        if (!properties.isEnabled()) {
            return false;
        }
        try {
            return meetingEventPublisher.publishMeetingReminder(reservation);
        } catch (Exception ex) {
            log.warn("notification-service reminder failed, reservationId={}: {}",
                    reservation.getReservationId(), ex.getMessage());
            return false;
        }
    }

    public boolean publishAnnouncement(ImportantNews announcement, Integer publisherId) {
        if (!properties.isEnabled()) {
            return false;
        }
        java.util.List<String> recipients = userServiceClient.listUserIds();
        if (recipients.isEmpty()) {
            log.warn("announcement not sent because no recipients were returned by user-service");
            return false;
        }
        try {
            return meetingEventPublisher.publishAnnouncement(announcement, publisherId, recipients);
        } catch (Exception ex) {
            log.warn("notification-service announcement publish failed: {}", ex.getMessage());
            return false;
        }
    }

}
