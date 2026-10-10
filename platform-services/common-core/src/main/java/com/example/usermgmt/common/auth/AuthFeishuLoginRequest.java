package com.example.usermgmt.common.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthFeishuLoginRequest {

    @NotBlank
    private String openId;

    private String unionId;
    private String userId;
    private String mobile;
    private String email;
    private String enterpriseEmail;
    private String name;
    private String avatarUrl;
    private String loginIp;
}
