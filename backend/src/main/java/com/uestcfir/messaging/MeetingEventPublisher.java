package com.uestcfir.messaging;

import com.uestcfir.client.UserServiceClient;
import com.uestcfir.pojo.entity.ImportantNews;
import com.uestcfir.pojo.entity.Reservations;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MeetingEventPublisher {

    private final MeetingEventOutboxService outboxService;
    private final UserServiceClient userServiceClient;

    public boolean publishAnnouncement(ImportantNews announcement, Integer publisherId, List<String> userIds) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("announcementId", announcement.getId());
        payload.put("userIds", userIds);
        payload.put("title", announcement.getTitle());
        payload.put("content", announcement.getContent());
        payload.put("category", "announcement");
        payload.put("link", announcement.getRelatedRoomId() == null ? "" : "/user/home?roomId=" + announcement.getRelatedRoomId());
        payload.put("creatorId", String.valueOf(publisherId));
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("newsType", announcement.getNewsType());
        params.put("severity", announcement.getSeverity());
        params.put("status", announcement.getStatus());
        params.put("startTime", announcement.getStartTime());
        params.put("endTime", announcement.getEndTime());
        params.put("relatedRoomId", announcement.getRelatedRoomId());
        payload.put("params", params);
        return outboxService.enqueue("announcement.published", payload);
    }

    public boolean publishMeetingReminder(Reservations reservation) {
        Map<String, Object> payload = reservationPayload(reservation);
        payload.put("userIds", List.of(canonicalUserId(reservation.getUserId())));
        payload.put("title", "会议提醒");
        payload.put("content", "您预约的会议即将在15分钟内开始");
        payload.put("category", "meeting_reminder");
        payload.put("link", "/user/meetingMinu");
        return outboxService.enqueue("reservation.reminder", payload);
    }

    public boolean publishReservationCreated(Reservations reservation) {
        return publishReservationEvent("reservation.created", reservation);
    }

    public boolean publishReservationApproved(Reservations reservation) {
        return publishReservationEvent("reservation.approved", reservation);
    }

    public boolean publishReservationRejected(Reservations reservation) {
        return publishReservationEvent("reservation.rejected", reservation);
    }

    public boolean publishReservationCancelled(Reservations reservation) {
        return publishReservationEvent("reservation.cancelled", reservation);
    }

    public boolean publishReservationExpired(Reservations reservation) {
        return publishReservationEvent("reservation.expired", reservation);
    }

    private boolean publishReservationEvent(String eventType, Reservations reservation) {
        Map<String, Object> payload = reservationPayload(reservation);
        payload.put("userId", canonicalUserId(reservation.getUserId()));
        payload.put("userIds", List.of(payload.get("userId")));
        return outboxService.enqueue(eventType, payload);
    }

    private Map<String, Object> reservationPayload(Reservations reservation) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reservationId", reservation.getReservationId());
        payload.put("legacyUserId", reservation.getUserId());
        payload.put("roomId", reservation.getRoomId());
        payload.put("roomName", reservation.getRoomName());
        payload.put("reserveDate", reservation.getReserveDate());
        payload.put("startTime", reservation.getStartTime());
        payload.put("endTime", reservation.getEndTime());
        payload.put("status", reservation.getStatus());
        payload.put("purpose", reservation.getPurpose());
        payload.put("rejectReason", reservation.getRejectReason());
        return payload;
    }

    private String canonicalUserId(Integer legacyUserId) {
        String userId = userServiceClient.resolveCanonicalUserId(legacyUserId);
        return userId == null || userId.isBlank() ? String.valueOf(legacyUserId) : userId;
    }
}
