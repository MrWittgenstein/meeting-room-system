package com.example.usermgmt.notification.controller;

import com.example.usermgmt.common.api.ApiResponse;
import com.example.usermgmt.notification.dto.CaptchaSendRequest;
import com.example.usermgmt.notification.dto.CaptchaSendResponse;
import com.example.usermgmt.notification.dto.CaptchaVerifyRequest;
import com.example.usermgmt.notification.dto.CaptchaVerifyResponse;
import com.example.usermgmt.notification.service.CaptchaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/captcha")
public class CaptchaController {

    private final CaptchaService captchaService;

    @PostMapping("/send")
    public ApiResponse<CaptchaSendResponse> send(@Valid @RequestBody CaptchaSendRequest request) {
        return ApiResponse.success("验证码已发送", captchaService.send(request));
    }

    @PostMapping("/verify")
    public ApiResponse<CaptchaVerifyResponse> verify(@Valid @RequestBody CaptchaVerifyRequest request) {
        return ApiResponse.success("验证码校验成功", captchaService.verify(request));
    }
}
