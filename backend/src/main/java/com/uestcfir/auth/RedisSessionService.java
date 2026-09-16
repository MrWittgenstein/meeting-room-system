package com.uestcfir.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uestcfir.exception.BusinessException;
import com.uestcfir.pojo.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisSessionService implements SessionService {
    private static final String KEY_PREFIX = "zongshe:session:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RbacService rbacService;

    @Value("${auth.session.ttl:12h}")
    private Duration ttl;

    @Override
    public SessionUser create(User user) {
        if (user == null || user.getUserId() == null) {
            throw new BusinessException("无法为无效用户创建会话");
        }
        LocalDateTime now = LocalDateTime.now();
        RbacService.AuthorizationSnapshot authorization =
                rbacService.resolve(user.getUserId(), user.getUserType());
        SessionUser sessionUser = SessionUser.builder()
                .sessionId(UUID.randomUUID().toString().replace("-", ""))
                .userId(user.getUserId())
                .username(user.getUsername())
                .userType(user.getUserType())
                .role(authorization.primaryRole())
                .permissions(authorization.permissions())
                .createdAt(now)
                .lastAccessAt(now)
                .build();
        save(sessionUser);
        return sessionUser;
    }

    @Override
    public SessionUser get(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new BusinessException("缺少 SessionId");
        }
        String json = redisTemplate.opsForValue().get(key(sessionId.trim()));
        if (json == null) {
            throw new BusinessException("会话不存在或已过期");
        }
        try {
            return objectMapper.readValue(json, SessionUser.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("会话数据无效");
        }
    }

    @Override
    public SessionUser touch(String sessionId) {
        SessionUser sessionUser = get(sessionId);
        sessionUser.setLastAccessAt(LocalDateTime.now());
        save(sessionUser);
        return sessionUser;
    }

    @Override
    public void invalidate(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            redisTemplate.delete(key(sessionId.trim()));
        }
    }

    private void save(SessionUser sessionUser) {
        try {
            redisTemplate.opsForValue().set(
                    key(sessionUser.getSessionId()),
                    objectMapper.writeValueAsString(sessionUser),
                    ttl
            );
        } catch (JsonProcessingException exception) {
            throw new BusinessException("会话保存失败");
        }
    }

    private String key(String sessionId) {
        return KEY_PREFIX + sessionId;
    }
}
