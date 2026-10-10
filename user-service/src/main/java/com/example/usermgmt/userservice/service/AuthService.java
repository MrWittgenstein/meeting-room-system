package com.example.usermgmt.userservice.service;

import com.example.usermgmt.common.auth.AuthCodeLoginRequest;
import com.example.usermgmt.common.auth.AuthFeishuLoginRequest;
import com.example.usermgmt.common.auth.AuthPasswordLoginRequest;
import com.example.usermgmt.common.auth.AuthRegisterRequest;
import com.example.usermgmt.common.auth.AuthResetPasswordRequest;
import com.example.usermgmt.common.auth.AuthenticatedUser;

public interface AuthService {

    AuthenticatedUser passwordLogin(AuthPasswordLoginRequest request);

    AuthenticatedUser codeLogin(AuthCodeLoginRequest request);

    AuthenticatedUser feishuLogin(AuthFeishuLoginRequest request);

    AuthenticatedUser register(AuthRegisterRequest request);

    void resetPasswordByAccount(AuthResetPasswordRequest request);

    boolean accountExists(String account);
}
