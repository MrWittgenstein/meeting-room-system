package com.example.usermgmt.common.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthPasswordLoginRequest {

    @NotBlank
    private String account;

    @NotBlank
    private String password;

    private String loginIp;
}
