package com.example.usermgmt.notification.service.impl;

import com.example.usermgmt.common.exception.BusinessException;
import com.example.usermgmt.notification.service.CarrierMailService;
import com.example.usermgmt.notification.service.CarrierSmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaDeliveryService {

    private final CarrierMailService carrierMailService;
    private final CarrierSmsService carrierSmsService;

    @Async("captchaSendExecutor")
    public void sendCaptchaAsync(String captchaType, String account, String scene, String code) {
        if ("email".equalsIgnoreCase(captchaType)) {
            sendEmailCaptcha(account, scene, code);
            return;
        }
        if ("sms".equalsIgnoreCase(captchaType)) {
            sendSmsCaptcha(account, scene, code);
        }
    }

    private void sendEmailCaptcha(String account, String scene, String code) {
        try {
            carrierMailService.sendTextEmailWithDefaultCarrier(
                account,
                "User Management Verification Code",
                buildMailText(scene, code)
            );
        } catch (Exception exception) {
            log.error("Failed to send email captcha to {}, scene={}", account, scene, exception);
        }
    }

    private String buildMailText(String scene, String code) {
        return "Scene: " + scene + System.lineSeparator()
            + "Code: " + code + System.lineSeparator()
            + "Expires in: 10 minutes" + System.lineSeparator()
            + "Do not share this code with anyone.";
    }

    private void sendSmsCaptcha(String account, String scene, String code) {
        try {
            carrierSmsService.sendCaptchaWithDefaultCarrier(account, scene, code);
        } catch (BusinessException exception) {
            log.error("Failed to send sms captcha to {}, scene={}", account, scene, exception);
        } catch (Exception exception) {
            log.error("Failed to send sms captcha to {}, scene={}", account, scene, exception);
        }
    }
}
