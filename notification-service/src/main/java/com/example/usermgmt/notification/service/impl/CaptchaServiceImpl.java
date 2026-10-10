package com.example.usermgmt.notification.service.impl;

import com.example.usermgmt.common.api.ApiResponse;
import com.example.usermgmt.common.accesslog.TraceContext;
import com.example.usermgmt.common.exception.BusinessException;
import com.example.usermgmt.notification.config.NotificationFeatureProperties;
import com.example.usermgmt.notification.dto.CaptchaSendRequest;
import com.example.usermgmt.notification.dto.CaptchaSendResponse;
import com.example.usermgmt.notification.dto.CaptchaVerifyRequest;
import com.example.usermgmt.notification.dto.CaptchaVerifyResponse;
import com.example.usermgmt.notification.service.CaptchaService;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import org.springframework.core.ParameterizedTypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Set<String> VALID_SCENES = Set.of("register", "login", "reset_password");
    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final Duration RESEND_TTL = Duration.ofSeconds(60);

    private final NotificationFeatureProperties featureProperties;
    private final StringRedisTemplate stringRedisTemplate;
    private final CaptchaDeliveryService captchaDeliveryService;

    @Value("${notification.clients.user-service-url:http://127.0.0.1:8181}")
    private String userServiceUrl;

    @Override
    public CaptchaSendResponse send(CaptchaSendRequest request) {
        validateBasicRequest(request.getCaptchaType(), request.getAccount(), request.getScene());
        validateSceneAccountState(request.getAccount(), request.getScene());

        String resendKey = resendKey(request.getCaptchaType(), request.getAccount(), request.getScene());
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(resendKey))) {
            throw new BusinessException(50005, "发送过于频繁，请稍后再试");
        }

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        stringRedisTemplate.opsForValue().set(codeKey(request.getCaptchaType(), request.getAccount(), request.getScene()), code, CODE_TTL);
        stringRedisTemplate.opsForValue().set(resendKey, "1", RESEND_TTL);
        captchaDeliveryService.sendCaptchaAsync(request.getCaptchaType(), request.getAccount(), request.getScene(), code);
        return new CaptchaSendResponse((int) CODE_TTL.toSeconds(), (int) RESEND_TTL.toSeconds());
    }

    @Override
    public CaptchaVerifyResponse verify(CaptchaVerifyRequest request) {
        validateBasicRequest(request.getCaptchaType(), request.getAccount(), request.getScene());
        String redisKey = codeKey(request.getCaptchaType(), request.getAccount(), request.getScene());
        String cached = stringRedisTemplate.opsForValue().get(redisKey);
        if (cached == null || !cached.equals(request.getCode())) {
            throw new BusinessException(40003, "验证码错误或已过期");
        }
        stringRedisTemplate.delete(redisKey);
        return new CaptchaVerifyResponse(Boolean.TRUE);
    }

    private void validateBasicRequest(String captchaType, String account, String scene) {
        if (!VALID_SCENES.contains(scene)) {
            throw new BusinessException(50001, "验证码类型无效");
        }
        if ("sms".equalsIgnoreCase(captchaType)) {
            if (!featureProperties.isSmsEnabled()) {
                throw new BusinessException(50007, "短信服务异常");
            }
            if (!PHONE_PATTERN.matcher(account).matches()) {
                throw new BusinessException(50002, "账号格式不正确");
            }
            return;
        }
        if ("email".equalsIgnoreCase(captchaType)) {
            if (!featureProperties.isEmailEnabled()) {
                throw new BusinessException(50008, "邮件服务异常");
            }
            if (!EMAIL_PATTERN.matcher(account).matches()) {
                throw new BusinessException(50002, "账号格式不正确");
            }
            return;
        }
        throw new BusinessException(50001, "验证码类型无效");
    }

    private void validateSceneAccountState(String account, String scene) {
        boolean exists = queryAccountExists(account);
        if ("register".equalsIgnoreCase(scene) && exists) {
            throw new BusinessException(50003, "账号已注册");
        }
        if (("login".equalsIgnoreCase(scene) || "reset_password".equalsIgnoreCase(scene)) && !exists) {
            throw new BusinessException(50004, "账号不存在");
        }
    }

    private boolean queryAccountExists(String account) {
        RestClient.Builder builder = RestClient.builder()
            .baseUrl(userServiceUrl);
        String traceId = TraceContext.getTraceId();
        if (traceId != null && !traceId.isBlank()) {
            builder.defaultHeader(TraceContext.TRACE_ID_HEADER, traceId);
        }
        ApiResponse<Boolean> response = builder.build()
            .post()
            .uri(uriBuilder -> uriBuilder.path("/internal/auth/account-exists").queryParam("account", account).build())
            .retrieve()
            .body(new ParameterizedTypeReference<ApiResponse<Boolean>>() {});
        if (response == null || response.getCode() == null || response.getCode() != 200 || response.getData() == null) {
            throw new BusinessException(500, "用户服务校验失败");
        }
        return Boolean.TRUE.equals(response.getData());
    }

    private String codeKey(String captchaType, String account, String scene) {
        return "notify:captcha:code:" + captchaType + ":" + scene + ":" + account;
    }

    private String resendKey(String captchaType, String account, String scene) {
        return "notify:captcha:resend:" + captchaType + ":" + scene + ":" + account;
    }
}
