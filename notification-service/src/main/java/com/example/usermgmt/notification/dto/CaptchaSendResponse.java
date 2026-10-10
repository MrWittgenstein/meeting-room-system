package com.example.usermgmt.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CaptchaSendResponse {

    private Integer expireSeconds;
    private Integer resendWaitSeconds;
}
