package com.example.usermgmt.userservice.controller;

import com.example.usermgmt.common.api.ApiResponse;
import com.example.usermgmt.common.auth.AuthCodeLoginRequest;
import com.example.usermgmt.common.auth.AuthFeishuLoginRequest;
import com.example.usermgmt.common.auth.AuthPasswordLoginRequest;
import com.example.usermgmt.common.auth.AuthRegisterRequest;
import com.example.usermgmt.common.auth.AuthResetPasswordRequest;
import com.example.usermgmt.common.auth.AuthenticatedUser;
import com.example.usermgmt.userservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/auth")
public class AuthInternalController {

    private final AuthService authService;

    @PostMapping("/password-login")
    public ApiResponse<AuthenticatedUser> passwordLogin(@Valid @RequestBody AuthPasswordLoginRequest request) {
        return ApiResponse.success(authService.passwordLogin(request));
    }

    @PostMapping("/code-login")
    public ApiResponse<AuthenticatedUser> codeLogin(@Valid @RequestBody AuthCodeLoginRequest request) {
        return ApiResponse.success("登录成功", authService.codeLogin(request));
    }

    @PostMapping("/feishu-login")
    public ApiResponse<AuthenticatedUser> feishuLogin(@Valid @RequestBody AuthFeishuLoginRequest request) {
        return ApiResponse.success("登录成功", authService.feishuLogin(request));
    }

    @PostMapping("/account-exists")
    public ApiResponse<Boolean> accountExists(@RequestParam("account") String account) {
        return ApiResponse.success(authService.accountExists(account));
    }

    @PostMapping("/register")
    public ApiResponse<AuthenticatedUser> register(@Valid @RequestBody AuthRegisterRequest request) {
        return ApiResponse.success("注册成功", authService.register(request));
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody AuthResetPasswordRequest request) {
        authService.resetPasswordByAccount(request);
        return ApiResponse.success("密码重置成功", null);
    }
}
