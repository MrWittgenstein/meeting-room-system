package com.example.usermgmt.notification.controller;

import com.example.usermgmt.common.api.ApiResponse;
import com.example.usermgmt.notification.dto.ChannelDtos;
import com.example.usermgmt.notification.service.ReusableMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/channels")
public class ChannelController {

    private final ReusableMessageService reusableMessageService;

    @PostMapping("/{channel}")
    public ApiResponse<ChannelDtos.ConfigureResponse> configure(
        @PathVariable("channel") String channel,
        @Valid @RequestBody ChannelDtos.ConfigureRequest request
    ) {
        return ApiResponse.success("Channel configured", reusableMessageService.configureChannel(channel, request));
    }

    @GetMapping("/status")
    public ApiResponse<ChannelDtos.StatusResponse> status() {
        return ApiResponse.success(reusableMessageService.channelStatus());
    }

    @DeleteMapping("/{channelId}")
    public ApiResponse<Void> delete(@PathVariable("channelId") String channelId) {
        reusableMessageService.deleteChannel(channelId);
        return ApiResponse.success("Channel deleted", null);
    }

    @PutMapping("/{channel}/switch")
    public ApiResponse<ChannelDtos.ConfigureResponse> switchProvider(
        @PathVariable("channel") String channel,
        @Valid @RequestBody ChannelDtos.SwitchRequest request
    ) {
        return ApiResponse.success("Channel switched", reusableMessageService.switchChannel(channel, request));
    }
}
