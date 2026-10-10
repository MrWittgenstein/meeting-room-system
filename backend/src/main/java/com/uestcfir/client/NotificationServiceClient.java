package com.uestcfir.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uestcfir.config.PlatformServiceProperties;
import com.uestcfir.pojo.entity.Reservations;
import com.uestcfir.pojo.entity.ImportantNews;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/** Adapter for notification-service. The meeting domain never owns delivery credentials. */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationServiceClient {
    private final RestTemplate platformRestTemplate;
    private final PlatformServiceProperties properties;
    private final ObjectMapper objectMapper;
    private final UserServiceClient userServiceClient;

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public boolean sendMeetingReminder(Reservations reservation) {
        if (!properties.isEnabled()) {
            return false;
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("userIds", java.util.List.of(userServiceClient.resolveCanonicalUserId(reservation.getUserId())));
        request.put("title", "会议提醒");
        request.put("content", "您预约的会议即将在15分钟内开始");
        request.put("category", "meeting_reminder");
        request.put("link", "/user/meetingMinu");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("reservationId", reservation.getReservationId());
        params.put("roomId", reservation.getRoomId());
        params.put("date", reservation.getReserveDate());
        params.put("startTime", reservation.getStartTime());
        request.put("params", objectMapper.valueToTree(params));
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (properties.getInternalToken() != null && !properties.getInternalToken().isBlank()) {
                headers.setBearerAuth(properties.getInternalToken());
            }
            platformRestTemplate.postForEntity(
                    notificationServiceBaseUrl() + "/internal/site-messages/send",
                    new HttpEntity<>(request, headers),
                    String.class);
            return true;
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
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("userIds", recipients);
        request.put("title", announcement.getTitle());
        request.put("content", announcement.getContent());
        request.put("category", "announcement");
        request.put("link", announcement.getRelatedRoomId() == null ? "" : "/user/home?roomId=" + announcement.getRelatedRoomId());
        request.put("creatorId", String.valueOf(publisherId));
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("newsType", announcement.getNewsType());
        params.put("severity", announcement.getSeverity());
        params.put("status", announcement.getStatus());
        params.put("startTime", announcement.getStartTime());
        params.put("endTime", announcement.getEndTime());
        params.put("relatedRoomId", announcement.getRelatedRoomId());
        request.put("params", objectMapper.valueToTree(params));
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (properties.getInternalToken() != null && !properties.getInternalToken().isBlank()) {
                headers.setBearerAuth(properties.getInternalToken());
            }
            platformRestTemplate.postForEntity(
                    notificationServiceBaseUrl() + "/internal/announcements/publish",
                    new HttpEntity<>(request, headers), String.class);
            return true;
        } catch (Exception ex) {
            log.warn("notification-service announcement publish failed: {}", ex.getMessage());
            return false;
        }
    }

    private String notificationServiceBaseUrl() {
        return properties.isDiscoveryEnabled()
                ? "http://" + properties.getNotificationServiceName()
                : properties.getNotificationServiceUrl();
    }
}
