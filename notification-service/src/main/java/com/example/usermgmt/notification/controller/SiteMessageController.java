package com.example.usermgmt.notification.controller;

import com.example.usermgmt.common.api.ApiResponse;
import com.example.usermgmt.notification.dto.SiteMessageListResponse;
import com.example.usermgmt.notification.dto.SiteMessageReadRequest;
import com.example.usermgmt.notification.dto.SiteMessageReadResponse;
import com.example.usermgmt.notification.dto.SiteMessageSendRequest;
import com.example.usermgmt.notification.service.SiteMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/site-messages")
public class SiteMessageController {

    private final SiteMessageService siteMessageService;

    @PostMapping("/send")
    public ApiResponse<Integer> send(@Valid @RequestBody SiteMessageSendRequest request) {
        return ApiResponse.success("站内消息发送成功", siteMessageService.send(request));
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<SiteMessageListResponse> list(
        @PathVariable("userId") String userId,
        @RequestParam(value = "page", defaultValue = "1") Integer page,
        @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
        @RequestParam(value = "readStatus", required = false) Boolean readStatus
    ) {
        return ApiResponse.success(siteMessageService.list(userId, page, pageSize, readStatus));
    }

    @PostMapping("/user/{userId}/read")
    public ApiResponse<SiteMessageReadResponse> markRead(
        @PathVariable("userId") String userId,
        @RequestBody SiteMessageReadRequest request
    ) {
        return ApiResponse.success("消息状态已更新", siteMessageService.markRead(userId, request));
    }
}
