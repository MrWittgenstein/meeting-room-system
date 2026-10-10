package com.example.usermgmt.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CaptchaVerifyRequest {

    @NotBlank
    private String captchaType;

    @NotBlank
    private String account;

    @NotBlank
    private String scene;

    @NotBlank
    private String code;
}
