package com.example.usermgmt.userservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.usermgmt.common.auth.AuthCodeLoginRequest;
import com.example.usermgmt.common.auth.AuthFeishuLoginRequest;
import com.example.usermgmt.common.auth.AuthPasswordLoginRequest;
import com.example.usermgmt.common.auth.AuthRegisterRequest;
import com.example.usermgmt.common.auth.AuthResetPasswordRequest;
import com.example.usermgmt.common.auth.AuthenticatedUser;
import com.example.usermgmt.common.enums.UserStatus;
import com.example.usermgmt.common.exception.BusinessException;
import com.example.usermgmt.userservice.config.UserServiceFeatureProperties;
import com.example.usermgmt.userservice.dto.auth.UserExtraData;
import com.example.usermgmt.userservice.dto.user.CreateUserRequest;
import com.example.usermgmt.userservice.dto.user.UserDetailResponse;
import com.example.usermgmt.userservice.entity.UserEntity;
import com.example.usermgmt.userservice.entity.UserGroupEntity;
import com.example.usermgmt.userservice.mapper.PermissionMapper;
import com.example.usermgmt.userservice.mapper.UserGroupMapper;
import com.example.usermgmt.userservice.mapper.LoginUserMapper;
import com.example.usermgmt.userservice.mapper.UserMapper;
import com.example.usermgmt.userservice.service.AuthService;
import com.example.usermgmt.userservice.service.LegacyEmailCodec;
import com.example.usermgmt.userservice.service.UserManagementService;
import com.example.usermgmt.userservice.support.BuiltInGroupConstants;
import com.example.usermgmt.userservice.util.PasswordCodec;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final BCryptPasswordEncoder LEGACY_BCRYPT = new BCryptPasswordEncoder();

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final UserMapper userMapper;
    private final PermissionMapper permissionMapper;
    private final UserGroupMapper userGroupMapper;
    private final LoginUserMapper loginUserMapper;
    private final ObjectMapper objectMapper;
    private final UserServiceFeatureProperties featureProperties;
    private final UserManagementService userManagementService;
    private final LegacyEmailCodec legacyEmailCodec;

    @Override
    public AuthenticatedUser passwordLogin(AuthPasswordLoginRequest request) {
        if (!featureProperties.isPasswordLoginEnabled()) {
            throw new BusinessException(40006, "密码登录功能当前未开启");
        }
        UserEntity user = requireUser(normalizeAccount(request.getAccount()));
        ensureUserAvailable(user);
        UserExtraData extraData = safeExtraData(user);
        if (isLocked(extraData)) {
            user.setStatus(UserStatus.LOCKED.getCode());
            user.setExtraData(UserExtraDataHelper.toJson(objectMapper, extraData));
            userMapper.updateById(user);
            throw new BusinessException(40005, "账号已被锁定，请稍后再试");
        }
        if (!matchesPassword(request.getPassword(), user)) {
            handlePasswordLoginFailure(user, extraData, request.getLoginIp());
            throw new BusinessException(isLocked(extraData) ? 40005 : 40002, isLocked(extraData) ? "账号已被锁定，请稍后再试" : "密码错误");
        }
        resetPasswordFailureState(user, extraData);
        recordLogin(user.getId(), "password", request.getLoginIp(), 1);
        return toAuthenticatedUser(user);
    }

    private boolean matchesPassword(String rawPassword, UserEntity user) {
        if (user.getPassword() != null && user.getPassword().startsWith("$2")) {
            return LEGACY_BCRYPT.matches(rawPassword, user.getPassword());
        }
        return PasswordCodec.matches(rawPassword, user.getSalt(), user.getPassword());
    }

    @Override
    public AuthenticatedUser codeLogin(AuthCodeLoginRequest request) {
        if (!featureProperties.isCodeLoginEnabled()) {
            throw new BusinessException(40006, "验证码登录功能当前未开启");
        }
        UserEntity user = userMapper.selectByAccount(request.getAccount());
        if (user == null) {
            String account = normalizeAccount(request.getAccount());
            CreateUserRequest createUserRequest = new CreateUserRequest();
            if (isEmailAccount(account)) {
                createUserRequest.setEmail(account);
            } else if (isPhoneAccount(account)) {
                createUserRequest.setPhone(account);
            } else {
                throw new BusinessException(40003, "账号格式不正确，请输入手机号或邮箱");
            }
            createUserRequest.setPassword("Temp123456");
            createUserRequest.setNickname(account);
            createUserRequest.setRole("user");
            createUserRequest.setStatus(UserStatus.ACTIVE.getCode());
            UserDetailResponse created = userManagementService.createUser(createUserRequest, null);
            user = userMapper.selectById(created.getUserId());
        } else {
            ensureUserAvailable(user);
        }
        recordLogin(user.getId(), isEmailAccount(request.getAccount()) ? "email_code" : "sms_code", request.getLoginIp(), 1);
        return toAuthenticatedUser(user);
    }

    @Override
    public AuthenticatedUser feishuLogin(AuthFeishuLoginRequest request) {
        if (!featureProperties.isOauthLoginEnabled()) {
            throw new BusinessException(40006, "第三方登录功能当前未开启");
        }
        UserEntity user = userMapper.selectByFeishuIdentity(
            request.getOpenId(),
            request.getUnionId(),
            request.getUserId(),
            UserStatus.CANCELLED.getCode()
        );
        if (user == null) {
            user = findExistingAccountForFeishu(request);
        }
        if (user == null) {
            user = createFeishuUser(request);
        }

        UserExtraData extraData = safeExtraData(user);
        extraData.setFeishuOpenId(request.getOpenId());
        extraData.setFeishuUnionId(request.getUnionId());
        extraData.setFeishuUserId(request.getUserId());
        if (StringUtils.hasText(request.getEnterpriseEmail())) {
            extraData.setEnterpriseEmail(request.getEnterpriseEmail());
        }
        if (StringUtils.hasText(request.getAvatarUrl())) {
            extraData.setAvatar(request.getAvatarUrl());
        }
        user.setExtraData(UserExtraDataHelper.toJson(objectMapper, extraData));
        if (StringUtils.hasText(request.getMobile()) && !StringUtils.hasText(user.getPhone())) {
            user.setPhone(request.getMobile());
        }
        if (StringUtils.hasText(request.getEnterpriseEmail()) && !StringUtils.hasText(user.getMail())) {
            user.setMail(request.getEnterpriseEmail());
        }
        if (StringUtils.hasText(request.getName())) {
            user.setName(request.getName());
        }
        userMapper.updateById(user);
        ensureUserAvailable(user);
        recordLogin(user.getId(), "oauth", request.getLoginIp(), 1);
        return toAuthenticatedUser(user);
    }

    @Override
    @Transactional
    public AuthenticatedUser register(AuthRegisterRequest request) {
        if (!featureProperties.isRegisterEnabled()) {
            throw new BusinessException(40006, "注册功能当前未开启");
        }
        String account = normalizeAccount(request.getAccount());
        CreateUserRequest createUserRequest = new CreateUserRequest();
        if (isPhoneAccount(account)) {
            createUserRequest.setPhone(account);
        } else if (isEmailAccount(account)) {
            createUserRequest.setEmail(account);
        } else {
            throw new BusinessException(40003, "账号格式不正确，请输入手机号或邮箱");
        }
        createUserRequest.setPassword(request.getPassword());
        createUserRequest.setNickname(request.getNickname());
        createUserRequest.setRole("user");
        createUserRequest.setStatus(UserStatus.ACTIVE.getCode());
        UserDetailResponse userDetail = userManagementService.createUser(createUserRequest, null);
        String loginAccount = userDetail.getPhone() != null ? userDetail.getPhone() : userDetail.getEmail();
        AuthPasswordLoginRequest loginRequest = new AuthPasswordLoginRequest();
        loginRequest.setAccount(loginAccount);
        loginRequest.setPassword(request.getPassword());
        loginRequest.setLoginIp(request.getLoginIp());
        return passwordLogin(loginRequest);
    }

    @Override
    public void resetPasswordByAccount(AuthResetPasswordRequest request) {
        String account = normalizeAccount(request.getAccount());
        UserEntity user = requireUser(account);
        ensureUserAvailable(user);
        user.setSalt(PasswordCodec.generateSalt());
        user.setPassword(PasswordCodec.encode(request.getNewPassword(), user.getSalt()));
        userMapper.updateById(user);
    }

    @Override
    public boolean accountExists(String account) {
        return userMapper.selectByAccount(normalizeAccount(account)) != null;
    }

    private UserEntity requireUser(String account) {
        UserEntity user = userMapper.selectByAccount(account);
        if (user == null && isEmailAccount(account)) {
            user = userMapper.selectByLegacyEmailHash(
                    legacyEmailCodec.sha256(account), legacyEmailCodec.md5(account));
        }
        if (user == null) {
            throw new BusinessException(40001, "账号不存在");
        }
        return user;
    }

    private void ensureUserAvailable(UserEntity user) {
        UserExtraData extraData = safeExtraData(user);
        if (user.getStatus() != null && user.getStatus() == UserStatus.LOCKED.getCode() && !isLocked(extraData)) {
            user.setStatus(UserStatus.ACTIVE.getCode());
            user.setExtraData(UserExtraDataHelper.toJson(objectMapper, extraData));
            userMapper.updateById(user);
        }
        if (UserStatus.DISABLED.getCode() == user.getStatus()) {
            throw new BusinessException(40004, "账号已被禁用");
        }
        if (UserStatus.LOCKED.getCode() == user.getStatus() && isLocked(extraData)) {
            throw new BusinessException(40005, "账号已被锁定，请稍后再试");
        }
    }

    private void handlePasswordLoginFailure(UserEntity user, UserExtraData extraData, String loginIp) {
        int attempts = extraData.getFailedPasswordAttempts() == null ? 0 : extraData.getFailedPasswordAttempts();
        attempts++;
        extraData.setFailedPasswordAttempts(attempts);
        if (attempts >= featureProperties.getPasswordMaxAttempts()) {
            extraData.setLockedUntil(java.time.LocalDateTime.now().plusMinutes(featureProperties.getPasswordLockMinutes()));
            user.setStatus(UserStatus.LOCKED.getCode());
        }
        user.setExtraData(UserExtraDataHelper.toJson(objectMapper, extraData));
        userMapper.updateById(user);
        recordLogin(user.getId(), "password", loginIp, 2);
    }

    private void resetPasswordFailureState(UserEntity user, UserExtraData extraData) {
        extraData.setFailedPasswordAttempts(0);
        extraData.setLockedUntil(null);
        if (user.getStatus() != null && user.getStatus() == UserStatus.LOCKED.getCode()) {
            user.setStatus(UserStatus.ACTIVE.getCode());
        }
        user.setExtraData(UserExtraDataHelper.toJson(objectMapper, extraData));
        userMapper.updateById(user);
    }

    private UserEntity createFeishuUser(AuthFeishuLoginRequest request) {
        UserExtraData extraData = new UserExtraData();
        extraData.setRole("user");
        extraData.setAvatar(request.getAvatarUrl());
        extraData.setFeishuOpenId(request.getOpenId());
        extraData.setFeishuUnionId(request.getUnionId());
        extraData.setFeishuUserId(request.getUserId());
        extraData.setEnterpriseEmail(request.getEnterpriseEmail());

        UserEntity entity = new UserEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setName(resolveFeishuDisplayName(request));
        entity.setPhone(blankToNull(request.getMobile()));
        entity.setMail(blankToNull(StringUtils.hasText(request.getEnterpriseEmail()) ? request.getEnterpriseEmail() : request.getEmail()));
        entity.setSalt(PasswordCodec.generateSalt());
        entity.setPassword(PasswordCodec.encode(UUID.randomUUID().toString(), entity.getSalt()));
        entity.setGender(0);
        entity.setDescription("Created by Feishu OAuth");
        entity.setStatus(UserStatus.ACTIVE.getCode());
        entity.setCreatorId("feishu-oauth");
        entity.setLastOperatorId("feishu-oauth");
        entity.setExtraData(UserExtraDataHelper.toJson(objectMapper, extraData));
        userMapper.insert(entity);
        assignNormalUserGroup(entity.getId());
        return entity;
    }

    private String resolveFeishuDisplayName(AuthFeishuLoginRequest request) {
        if (StringUtils.hasText(request.getName())) {
            return request.getName();
        }
        if (StringUtils.hasText(request.getEnterpriseEmail())) {
            return request.getEnterpriseEmail();
        }
        if (StringUtils.hasText(request.getEmail())) {
            return request.getEmail();
        }
        return "feishu-" + request.getOpenId();
    }

    private void assignNormalUserGroup(String userId) {
        QueryWrapper<UserGroupEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId).eq("group_id", BuiltInGroupConstants.NORMAL_USER_GROUP_ID);
        if (userGroupMapper.selectCount(queryWrapper) > 0) {
            return;
        }
        UserGroupEntity relation = new UserGroupEntity();
        relation.setUserId(userId);
        relation.setGroupId(BuiltInGroupConstants.NORMAL_USER_GROUP_ID);
        relation.setCreatorId("feishu-oauth");
        relation.setLastOperatorId("feishu-oauth");
        relation.setDescription("Default normal user group");
        relation.setType("member");
        userGroupMapper.insert(relation);
    }

    private UserEntity findExistingAccountForFeishu(AuthFeishuLoginRequest request) {
        String account = StringUtils.hasText(request.getEnterpriseEmail()) ? request.getEnterpriseEmail() : request.getEmail();
        if (StringUtils.hasText(account)) {
            UserEntity user = userMapper.selectByAccount(account);
            if (user != null && user.getStatus() != null && user.getStatus() != UserStatus.CANCELLED.getCode()) {
                return user;
            }
        }
        if (StringUtils.hasText(request.getMobile())) {
            UserEntity user = userMapper.selectByAccount(request.getMobile());
            if (user != null && user.getStatus() != null && user.getStatus() != UserStatus.CANCELLED.getCode()) {
                return user;
            }
        }
        return null;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private boolean isLocked(UserExtraData extraData) {
        return extraData != null
            && extraData.getLockedUntil() != null
            && extraData.getLockedUntil().isAfter(java.time.LocalDateTime.now());
    }

    private void recordLogin(String userId, String credentialType, String loginIp, int loginResult) {
        loginUserMapper.insertLoginRecord(
            UUID.randomUUID().toString().replace("-", ""),
            credentialType,
            userId,
            loginIp,
            loginResult
        );
    }

    private AuthenticatedUser toAuthenticatedUser(UserEntity user) {
        UserExtraData extraData = safeExtraData(user);
        AuthenticatedUser authenticatedUser = new AuthenticatedUser();
        authenticatedUser.setUserId(user.getId());
        authenticatedUser.setNickname(user.getName());
        authenticatedUser.setAvatar(extraData.getAvatar());
        authenticatedUser.setStatus(user.getStatus());
        String role = resolveUserRole(user.getId());
        authenticatedUser.setRole(role);
        List<String> menus = "admin".equals(role)
            ? permissionMapper.selectAllActivePermissionCodes()
            : permissionMapper.selectEffectiveMenuNames(user.getId());
        authenticatedUser.setMenus(menus);
        return authenticatedUser;
    }

    private String resolveUserRole(String userId) {
        QueryWrapper<UserGroupEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId).eq("group_id", BuiltInGroupConstants.ADMIN_GROUP_ID);
        return userGroupMapper.selectCount(queryWrapper) > 0 ? "admin" : "user";
    }

    private boolean isPhoneAccount(String account) {
        return StringUtils.hasText(account) && PHONE_PATTERN.matcher(account).matches();
    }

    private boolean isEmailAccount(String account) {
        return StringUtils.hasText(account) && EMAIL_PATTERN.matcher(account).matches();
    }

    private String normalizeAccount(String account) {
        if (account == null) {
            return null;
        }
        String normalized = account.trim();
        return isEmailAccount(normalized) ? normalized.toLowerCase(Locale.ROOT) : normalized;
    }

    private UserExtraData safeExtraData(UserEntity user) {
        UserExtraData extraData = UserExtraDataHelper.parse(objectMapper, user.getExtraData());
        return extraData == null ? new UserExtraData() : extraData;
    }
}
