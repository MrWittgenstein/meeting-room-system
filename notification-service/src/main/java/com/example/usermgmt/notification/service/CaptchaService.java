package com.example.usermgmt.notification.service;

import com.example.usermgmt.notification.dto.CaptchaSendRequest;
import com.example.usermgmt.notification.dto.CaptchaSendResponse;
import com.example.usermgmt.notification.dto.CaptchaVerifyRequest;
import com.example.usermgmt.notification.dto.CaptchaVerifyResponse;

public interface CaptchaService {

    CaptchaSendResponse send(CaptchaSendRequest request);

    CaptchaVerifyResponse verify(CaptchaVerifyRequest request);
}
