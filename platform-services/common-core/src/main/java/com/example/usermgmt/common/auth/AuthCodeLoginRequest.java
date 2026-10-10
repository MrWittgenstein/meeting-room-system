package com.example.usermgmt.common.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthCodeLoginRequest {

    @NotBlank
    private String account;

    private String loginIp;
}
