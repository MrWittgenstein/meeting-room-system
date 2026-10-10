package com.example.usermgmt.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CaptchaVerifyResponse {

    private Boolean verified;
}
