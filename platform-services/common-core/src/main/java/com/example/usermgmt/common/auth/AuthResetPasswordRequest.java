package com.example.usermgmt.common.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthResetPasswordRequest {

    @NotBlank
    private String account;

    @NotBlank
    private String newPassword;
}
