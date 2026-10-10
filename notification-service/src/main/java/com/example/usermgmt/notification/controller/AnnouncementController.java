package com.example.usermgmt.notification.controller;

import com.example.usermgmt.common.api.ApiResponse;
import com.example.usermgmt.notification.dto.AnnouncementPublishRequest;
import com.example.usermgmt.notification.dto.SiteMessageSendRequest;
import com.example.usermgmt.notification.service.SiteMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stable announcement boundary. Persistence and recipient fan-out remain in the
 * notification service; meeting-room only supplies business content and user IDs.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/announcements")
public class AnnouncementController {
    private final SiteMessageService siteMessageService;

    @PostMapping("/publish")
    public ApiResponse<Integer> publish(@Valid @RequestBody AnnouncementPublishRequest request) {
        SiteMessageSendRequest message = new SiteMessageSendRequest();
        message.setUserIds(request.getUserIds());
        message.setTitle(request.getTitle());
        message.setContent(request.getContent());
        message.setCategory(request.getCategory() == null ? "announcement" : request.getCategory());
        message.setLink(request.getLink());
        message.setCreatorId(request.getCreatorId());
        message.setParams(request.getParams());
        return ApiResponse.success("公告发布成功", siteMessageService.send(message));
    }
}
