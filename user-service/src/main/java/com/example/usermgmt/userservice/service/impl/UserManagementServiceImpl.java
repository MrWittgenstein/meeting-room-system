package com.example.usermgmt.userservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.usermgmt.common.api.PageResponse;
import com.example.usermgmt.common.enums.UserStatus;
import com.example.usermgmt.common.exception.BusinessException;
import com.example.usermgmt.userservice.dto.common.BatchOperationResponse;
import com.example.usermgmt.userservice.dto.common.BatchFailItemResponse;
import com.example.usermgmt.userservice.dto.auth.UserAttributeExtraData;
import com.example.usermgmt.userservice.dto.auth.UserExtraData;
import com.example.usermgmt.userservice.dto.user.BatchUpdateUsersResponse;
import com.example.usermgmt.userservice.dto.user.ChangePasswordRequest;
import com.example.usermgmt.userservice.dto.user.CreateUserRequest;
import com.example.usermgmt.userservice.dto.user.FailItemResponse;
import com.example.usermgmt.userservice.dto.user.FeishuIdentityResponse;
import com.example.usermgmt.userservice.dto.user.ResetPasswordRequest;
import com.example.usermgmt.userservice.dto.user.UpdateCurrentUserRequest;
import com.example.usermgmt.userservice.dto.user.UpdateUsersRequest;
import com.example.usermgmt.userservice.dto.user.UserAttributeRequest;
import com.example.usermgmt.userservice.dto.user.UserAttributeResponse;
import com.example.usermgmt.userservice.dto.user.UserDetailResponse;
import com.example.usermgmt.userservice.dto.user.UserListItemResponse;
import com.example.usermgmt.userservice.dto.user.UserLookupResponse;
import com.example.usermgmt.userservice.dto.user.UserQueryRequest;
import com.example.usermgmt.userservice.entity.AttributeDataEntity;
import com.example.usermgmt.userservice.entity.UserEntity;
import com.example.usermgmt.userservice.entity.UserGroupEntity;
import com.example.usermgmt.userservice.mapper.AttributeDataMapper;
import com.example.usermgmt.userservice.mapper.LoginUserMapper;
import com.example.usermgmt.userservice.mapper.PermissionUserMapper;
import com.example.usermgmt.userservice.mapper.PermissionMapper;
import com.example.usermgmt.userservice.mapper.UserGroupMapper;
import com.example.usermgmt.userservice.mapper.UserMapper;
import com.example.usermgmt.userservice.service.UserManagementService;
import com.example.usermgmt.userservice.support.BuiltInGroupConstants;
import com.example.usermgmt.userservice.util.PasswordCodec;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    private final AttributeDataMapper attributeDataMapper;
    private final LoginUserMapper loginUserMapper;
    private final UserMapper userMapper;
    private final UserGroupMapper userGroupMapper;
    private final PermissionUserMapper permissionUserMapper;
    private final PermissionMapper permissionMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public UserDetailResponse createUser(CreateUserRequest request, String operatorId) {
        validateCreateRequest(request);
        ensureUniquePhone(request.getPhone(), null);
        ensureUniqueMail(request.getEmail(), null);

        UserExtraData extraData = new UserExtraData();
        extraData.setAvatar(request.getAvatar());
        extraData.setRole(StringUtils.hasText(request.getRole()) ? request.getRole() : "user");
        extraData.setRemark(request.getRemark());

        UserEntity entity = new UserEntity();
        entity.setId(generateId());
        entity.setName(resolveDisplayName(request.getNickname(), request.getPhone(), request.getEmail()));
        entity.setPhone(blankToNull(request.getPhone()));
        entity.setMail(blankToNull(request.getEmail()));
        entity.setSalt(PasswordCodec.generateSalt());
        entity.setPassword(PasswordCodec.encode(request.getPassword(), entity.getSalt()));
        entity.setGender(0);
        entity.setDescription(request.getRemark());
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        entity.setCreatorId(operatorId);
        entity.setLastOperatorId(operatorId);
        entity.setExtraData(UserExtraDataHelper.toJson(objectMapper, extraData));
        userMapper.insert(entity);
        assignNormalUserGroupIfNeeded(entity.getId(), operatorId, extraData.getRole());
        return toDetailResponse(entity);
    }

    @Override
    public PageResponse<UserListItemResponse> listUsers(UserQueryRequest request) {
        int page = request.getPage() == null || request.getPage() < 1 ? 1 : request.getPage();
        int pageSize = request.getPageSize() == null || request.getPageSize() < 1 ? 20 : Math.min(request.getPageSize(), 100);
        int cancelledStatus = UserStatus.CANCELLED.getCode();
        long total = userMapper.countUsers(request.getKeyword(), request.getStatus(), request.getRole(), cancelledStatus);
        long offset = (long) (page - 1) * pageSize;
        List<UserEntity> entities = userMapper.selectUsers(
            request.getKeyword(),
            request.getStatus(),
            request.getRole(),
            cancelledStatus,
            offset,
            pageSize
        );
        List<UserListItemResponse> responses = entities.stream().map(this::toListItemResponse).toList();
        return PageResponse.<UserListItemResponse>builder()
            .total(total)
            .page(page)
            .pageSize(pageSize)
            .list(responses)
            .build();
    }

    @Override
    public List<UserLookupResponse> lookupUsers(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        List<UserEntity> entities = userMapper.selectUsers(
            keyword.trim(),
            UserStatus.ACTIVE.getCode(),
            null,
            UserStatus.CANCELLED.getCode(),
            0,
            20
        );
        return entities.stream().map(this::toLookupResponse).toList();
    }

    @Override
    public UserDetailResponse getUserDetail(String userId) {
        return toDetailResponse(requireUser(userId));
    }

    @Override
    public String findUserIdByLegacyUserId(Integer legacyUserId) {
        if (legacyUserId == null) {
            return null;
        }
        UserEntity entity = userMapper.selectByLegacyUserId(legacyUserId);
        return entity == null ? null : entity.getId();
    }

    @Override
    public FeishuIdentityResponse getFeishuIdentity(String userId) {
        UserEntity user = requireUser(userId);
        UserExtraData extraData = UserExtraDataHelper.parse(objectMapper, user.getExtraData());
        FeishuIdentityResponse response = new FeishuIdentityResponse();
        response.setUserId(user.getId());
        response.setFeishuOpenId(extraData.getFeishuOpenId());
        response.setFeishuUserId(extraData.getFeishuUserId());
        response.setFeishuUnionId(extraData.getFeishuUnionId());
        response.setEnterpriseEmail(extraData.getEnterpriseEmail());
        return response;
    }

    @Override
    public UserDetailResponse getCurrentUser(String currentUserId) {
        return getUserDetail(currentUserId);
    }

    @Override
    public BatchUpdateUsersResponse updateUsers(UpdateUsersRequest request, String operatorId) {
        if (!hasAnyUpdateField(request)) {
            throw new BusinessException(40010, "至少提供一个更新字段");
        }
        List<UserDetailResponse> successList = new ArrayList<>();
        List<FailItemResponse> failList = new ArrayList<>();
        for (String userId : request.getUserIds()) {
            UserEntity entity = userMapper.selectById(userId);
            if (entity == null) {
                failList.add(new FailItemResponse(userId, "用户不存在"));
                continue;
            }
            if (StringUtils.hasText(request.getPhone())) {
                ensureUniquePhone(request.getPhone(), entity.getId());
                entity.setPhone(request.getPhone());
            }
            if (StringUtils.hasText(request.getEmail())) {
                ensureUniqueMail(request.getEmail(), entity.getId());
                entity.setMail(request.getEmail());
            }
            if (StringUtils.hasText(request.getNickname())) {
                entity.setName(request.getNickname());
            }
            if (request.getStatus() != null) {
                entity.setStatus(request.getStatus());
            }
            if (StringUtils.hasText(request.getRemark())) {
                entity.setDescription(request.getRemark());
            }

            UserExtraData extraData = UserExtraDataHelper.parse(objectMapper, entity.getExtraData());
            if (StringUtils.hasText(request.getAvatar())) {
                extraData.setAvatar(request.getAvatar());
            }
            if (StringUtils.hasText(request.getRole())) {
                extraData.setRole(request.getRole());
            }
            if (StringUtils.hasText(request.getRemark())) {
                extraData.setRemark(request.getRemark());
            }
            entity.setExtraData(UserExtraDataHelper.toJson(objectMapper, extraData));
            entity.setLastOperatorId(operatorId);
            userMapper.updateById(entity);
            successList.add(toDetailResponse(entity));
        }

        BatchUpdateUsersResponse response = new BatchUpdateUsersResponse();
        response.setSuccessCount(successList.size());
        response.setFailCount(failList.size());
        response.setSuccessList(successList);
        response.setFailList(failList);
        return response;
    }

    @Override
    public UserDetailResponse updateCurrentUser(String currentUserId, UpdateCurrentUserRequest request) {
        UserEntity entity = requireUser(currentUserId);
        if (StringUtils.hasText(request.getNickname())) {
            entity.setName(request.getNickname());
        }
        UserExtraData extraData = UserExtraDataHelper.parse(objectMapper, entity.getExtraData());
        if (StringUtils.hasText(request.getAvatar())) {
            extraData.setAvatar(request.getAvatar());
        }
        entity.setExtraData(UserExtraDataHelper.toJson(objectMapper, extraData));
        userMapper.updateById(entity);
        return toDetailResponse(entity);
    }

    @Override
    public void changePassword(String currentUserId, ChangePasswordRequest request) {
        UserEntity entity = requireUser(currentUserId);
        if (!PasswordCodec.matches(request.getOldPassword(), entity.getSalt(), entity.getPassword())) {
            throw new BusinessException(40001, "原密码错误");
        }
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BusinessException(40003, "新密码不能与原密码相同");
        }
        entity.setSalt(PasswordCodec.generateSalt());
        entity.setPassword(PasswordCodec.encode(request.getNewPassword(), entity.getSalt()));
        userMapper.updateById(entity);
    }

    @Override
    public String resetPassword(String targetUserId, ResetPasswordRequest request, String operatorId) {
        UserEntity entity = requireUser(targetUserId);
        String newPassword = StringUtils.hasText(request.getNewPassword()) ? request.getNewPassword() : "ChangeMe123";
        entity.setSalt(PasswordCodec.generateSalt());
        entity.setPassword(PasswordCodec.encode(newPassword, entity.getSalt()));
        entity.setLastOperatorId(operatorId);
        userMapper.updateById(entity);
        return newPassword;
    }

    @Override
    public List<String> getCurrentMenus(String currentUserId) {
        if (isAdminUser(currentUserId)) {
            return permissionMapper.selectAllActivePermissionCodes();
        }
        return permissionMapper.selectEffectiveMenuNames(currentUserId);
    }

    @Override
    public java.time.LocalDateTime deleteUser(String userId, boolean permanent, String operatorId) {
        UserEntity entity = requireUser(userId);
        java.time.LocalDateTime deletedAt = java.time.LocalDateTime.now();
        if (permanent) {
            attributeDataMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AttributeDataEntity>().eq("user_id", userId));
            permissionUserMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.example.usermgmt.userservice.entity.PermissionUserEntity>().eq("user_id", userId));
            userGroupMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.example.usermgmt.userservice.entity.UserGroupEntity>().eq("user_id", userId));
            loginUserMapper.deleteByUserId(userId);
            userMapper.deleteById(userId);
            return deletedAt;
        }

        userMapper.cancelUser(userId, UserStatus.CANCELLED.getCode(), operatorId);
        attributeDataMapper.delete(new QueryWrapper<AttributeDataEntity>().eq("user_id", userId));
        permissionUserMapper.delete(new QueryWrapper<com.example.usermgmt.userservice.entity.PermissionUserEntity>().eq("user_id", userId));
        userGroupMapper.delete(new QueryWrapper<UserGroupEntity>().eq("user_id", userId));
        loginUserMapper.deleteByUserId(userId);
        return deletedAt;
    }

    @Override
    public BatchOperationResponse batchDeleteUsers(List<String> userIds, String operatorId) {
        BatchOperationResponse response = new BatchOperationResponse();
        java.util.ArrayList<BatchFailItemResponse> failList = new java.util.ArrayList<>();
        int success = 0;
        for (String userId : userIds) {
            try {
                deleteUser(userId, false, operatorId);
                success++;
            } catch (BusinessException exception) {
                failList.add(BatchFailItemResponse.user(userId, exception.getMessage()));
            }
        }
        response.setSuccessCount(success);
        response.setFailCount(failList.size());
        response.setFailList(failList);
        return response;
    }

    @Override
    public UserAttributeResponse createUserAttribute(String userId, UserAttributeRequest request) {
        requireUser(userId);
        validateAttributeRequest(request, null, userId);

        AttributeDataEntity entity = new AttributeDataEntity();
        entity.setId(generateId());
        entity.setGroupId(null);
        entity.setUserId(userId);
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setExtraData(toAttributeExtraJson(request));
        attributeDataMapper.insert(entity);
        return toUserAttributeResponse(entity);
    }

    @Override
    public UserAttributeResponse updateUserAttribute(String userId, String attributeId, UserAttributeRequest request) {
        requireUser(userId);
        AttributeDataEntity entity = requireUserAttribute(userId, attributeId);
        validateAttributeRequest(request, attributeId, userId);

        if (StringUtils.hasText(request.getName())) {
            entity.setName(request.getName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }

        UserAttributeResponse current = toUserAttributeResponse(entity);
        UserAttributeExtraData extraData = new UserAttributeExtraData();
        extraData.setFieldKey(StringUtils.hasText(request.getFieldKey()) ? request.getFieldKey() : current.getFieldKey());
        extraData.setFieldType(StringUtils.hasText(request.getFieldType()) ? request.getFieldType() : current.getFieldType());
        extraData.setRequired(request.getRequired() != null ? request.getRequired() : current.getRequired());
        extraData.setValue(request.getValue() != null ? request.getValue() : current.getValue());
        extraData.setDefaultValue(request.getDefaultValue() != null ? request.getDefaultValue() : current.getDefaultValue());
        extraData.setOptions(request.getOptions() != null ? request.getOptions() : current.getOptions());
        extraData.setSort(request.getSort() != null ? request.getSort() : current.getSort());
        entity.setExtraData(UserExtraDataHelper.toJson(objectMapper, extraData));

        attributeDataMapper.updateById(entity);
        return toUserAttributeResponse(entity);
    }

    @Override
    public void deleteUserAttribute(String userId, String attributeId) {
        requireUserAttribute(userId, attributeId);
        attributeDataMapper.deleteById(attributeId);
    }

    @Override
    public List<UserAttributeResponse> listUserAttributes(String userId) {
        requireUser(userId);
        return attributeDataMapper.selectUserAttributes(userId).stream()
            .map(this::toUserAttributeResponse)
            .sorted(Comparator.comparing(item -> item.getSort() == null ? Integer.MAX_VALUE : item.getSort()))
            .toList();
    }

    private void validateCreateRequest(CreateUserRequest request) {
        if (!StringUtils.hasText(request.getPhone()) && !StringUtils.hasText(request.getEmail())) {
            throw new BusinessException(40003, "手机号和邮箱至少填写一个");
        }
    }

    private void assignNormalUserGroupIfNeeded(String userId, String operatorId, String role) {
        if ("admin".equalsIgnoreCase(role)) {
            return;
        }
        QueryWrapper<UserGroupEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId).eq("group_id", BuiltInGroupConstants.NORMAL_USER_GROUP_ID);
        if (userGroupMapper.selectCount(queryWrapper) > 0) {
            return;
        }
        String resolvedOperatorId = StringUtils.hasText(operatorId) ? operatorId : "system";
        UserGroupEntity relation = new UserGroupEntity();
        relation.setUserId(userId);
        relation.setGroupId(BuiltInGroupConstants.NORMAL_USER_GROUP_ID);
        relation.setCreatorId(resolvedOperatorId);
        relation.setLastOperatorId(resolvedOperatorId);
        relation.setDescription("Default normal user group");
        relation.setType("member");
        userGroupMapper.insert(relation);
    }

    private boolean hasAnyUpdateField(UpdateUsersRequest request) {
        return StringUtils.hasText(request.getNickname())
            || StringUtils.hasText(request.getAvatar())
            || StringUtils.hasText(request.getPhone())
            || StringUtils.hasText(request.getEmail())
            || StringUtils.hasText(request.getRole())
            || request.getStatus() != null
            || StringUtils.hasText(request.getRemark());
    }

    private void ensureUniquePhone(String phone, String excludeId) {
        if (StringUtils.hasText(phone) && userMapper.countByPhone(phone, excludeId) > 0) {
            throw new BusinessException(40001, "手机号已存在");
        }
    }

    private void ensureUniqueMail(String mail, String excludeId) {
        if (StringUtils.hasText(mail) && userMapper.countByMail(mail, excludeId) > 0) {
            throw new BusinessException(40002, "邮箱已存在");
        }
    }

    private UserEntity requireUser(String userId) {
        UserEntity entity = userMapper.selectById(userId);
        if (entity == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return entity;
    }

    private boolean isAdminUser(String userId) {
        QueryWrapper<UserGroupEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId).eq("group_id", BuiltInGroupConstants.ADMIN_GROUP_ID);
        return userGroupMapper.selectCount(queryWrapper) > 0;
    }

    private AttributeDataEntity requireUserAttribute(String userId, String attributeId) {
        AttributeDataEntity entity = attributeDataMapper.selectById(attributeId);
        if (entity == null || entity.getUserId() == null || !userId.equals(entity.getUserId())) {
            throw new BusinessException(404, "用户属性不存在");
        }
        return entity;
    }

    private void validateAttributeRequest(UserAttributeRequest request, String attributeId, String userId) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BusinessException(400, "属性名称不能为空");
        }
        if (!StringUtils.hasText(request.getFieldKey())) {
            throw new BusinessException(400, "属性键不能为空");
        }
        List<AttributeDataEntity> existed = attributeDataMapper.selectUserAttributes(userId);
        for (AttributeDataEntity existedEntity : existed) {
            if (attributeId != null && attributeId.equals(existedEntity.getId())) {
                continue;
            }
            UserAttributeResponse existedResponse = toUserAttributeResponse(existedEntity);
            if (request.getFieldKey().equals(existedResponse.getFieldKey())) {
                throw new BusinessException(40001, "属性键已存在");
            }
        }
    }

    private String toAttributeExtraJson(UserAttributeRequest request) {
        UserAttributeExtraData extraData = new UserAttributeExtraData();
        extraData.setFieldKey(request.getFieldKey());
        extraData.setFieldType(StringUtils.hasText(request.getFieldType()) ? request.getFieldType() : "string");
        extraData.setRequired(Boolean.TRUE.equals(request.getRequired()));
        extraData.setValue(request.getValue());
        extraData.setDefaultValue(request.getDefaultValue());
        extraData.setOptions(request.getOptions());
        extraData.setSort(request.getSort() == null ? 0 : request.getSort());
        return UserExtraDataHelper.toJson(objectMapper, extraData);
    }

    private UserAttributeResponse toUserAttributeResponse(AttributeDataEntity entity) {
        UserAttributeExtraData extraData = UserExtraDataHelper.parse(objectMapper, entity.getExtraData(), UserAttributeExtraData.class);
        UserAttributeResponse response = new UserAttributeResponse();
        response.setAttributeId(entity.getId());
        response.setUserId(entity.getUserId());
        response.setName(entity.getName());
        response.setFieldKey(extraData == null ? null : extraData.getFieldKey());
        response.setFieldType(extraData == null ? null : extraData.getFieldType());
        response.setRequired(extraData != null && Boolean.TRUE.equals(extraData.getRequired()));
        response.setValue(extraData == null ? null : extraData.getValue());
        response.setDefaultValue(extraData == null ? null : extraData.getDefaultValue());
        response.setOptions(extraData == null ? null : extraData.getOptions());
        response.setSort(extraData == null ? null : extraData.getSort());
        response.setDescription(entity.getDescription());
        response.setCreatedAt(entity.getCreateTime());
        response.setUpdatedAt(entity.getLastUpdateTime());
        return response;
    }

    private UserDetailResponse toDetailResponse(UserEntity entity) {
        UserExtraData extraData = UserExtraDataHelper.parse(objectMapper, entity.getExtraData());
        Map<String, Object> latestLogin = loginUserMapper.selectLatestSuccessByUserId(entity.getId());
        UserDetailResponse response = new UserDetailResponse();
        response.setUserId(entity.getId());
        response.setNickname(entity.getName());
        response.setAvatar(extraData.getAvatar());
        response.setPhone(entity.getPhone());
        response.setEmail(entity.getMail());
        response.setRole(extraData.getRole() == null ? "user" : extraData.getRole());
        response.setGroupIds(collectUserGroupIds(entity.getId()));
        response.setGroupNames(collectUserGroupNames(entity.getId()));
        response.setStatus(toUserStatus(entity.getStatus()));
        response.setRemark(extraData.getRemark() == null ? entity.getDescription() : extraData.getRemark());
        response.setLastLoginAt(readLoginTime(latestLogin));
        response.setLastLoginIp(readString(latestLogin, "loginIp"));
        response.setCreatedAt(entity.getCreateTime());
        response.setUpdatedAt(entity.getLastUpdateTime());
        response.setAttributes(listUserAttributes(entity.getId()));
        return response;
    }

    private UserListItemResponse toListItemResponse(UserEntity entity) {
        UserExtraData extraData = UserExtraDataHelper.parse(objectMapper, entity.getExtraData());
        Map<String, Object> latestLogin = loginUserMapper.selectLatestSuccessByUserId(entity.getId());
        UserListItemResponse response = new UserListItemResponse();
        response.setUserId(entity.getId());
        response.setNickname(entity.getName());
        response.setAvatar(extraData.getAvatar());
        response.setPhone(maskPhone(entity.getPhone()));
        response.setEmail(maskEmail(entity.getMail()));
        response.setRole(extraData.getRole() == null ? "user" : extraData.getRole());
        response.setGroupNames(collectUserGroupNames(entity.getId()));
        response.setStatus(toUserStatus(entity.getStatus()));
        response.setLastLoginAt(readLoginTime(latestLogin));
        response.setCreatedAt(entity.getCreateTime());
        return response;
    }

    private UserLookupResponse toLookupResponse(UserEntity entity) {
        UserExtraData extraData = UserExtraDataHelper.parse(objectMapper, entity.getExtraData());
        UserLookupResponse response = new UserLookupResponse();
        response.setUserId(entity.getId());
        response.setNickname(entity.getName());
        response.setAvatar(extraData.getAvatar());
        response.setPhone(entity.getPhone());
        response.setEmail(entity.getMail());
        response.setFeishuOpenId(extraData.getFeishuOpenId());
        response.setFeishuUserId(extraData.getFeishuUserId());
        response.setFeishuUnionId(extraData.getFeishuUnionId());
        return response;
    }

    private List<String> collectUserGroupNames(String userId) {
        return userGroupMapper.selectUserGroups(userId).stream()
            .map(row -> row.name)
            .filter(StringUtils::hasText)
            .toList();
    }

    private List<String> collectUserGroupIds(String userId) {
        return userGroupMapper.selectUserGroups(userId).stream()
            .map(row -> row.id)
            .filter(StringUtils::hasText)
            .toList();
    }

    private String resolveDisplayName(String nickname, String phone, String email) {
        if (StringUtils.hasText(nickname)) {
            return nickname;
        }
        if (StringUtils.hasText(phone)) {
            return phone;
        }
        return email;
    }

    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private String toUserStatus(Integer status) {
        return switch (UserStatus.fromCode(status)) {
            case ACTIVE -> "active";
            case DISABLED -> "disabled";
            case LOCKED -> "locked";
            case CANCELLED -> "cancelled";
        };
    }

    private LocalDateTime readLoginTime(Map<String, Object> latestLogin) {
        if (latestLogin == null) {
            return null;
        }
        Object value = latestLogin.get("loginTime");
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        return null;
    }

    private String readString(Map<String, Object> source, String key) {
        if (source == null || source.get(key) == null) {
            return null;
        }
        return String.valueOf(source.get(key));
    }

    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        if (local.length() <= 2) {
            return local.charAt(0) + "***@" + parts[1];
        }
        return local.substring(0, Math.min(3, local.length())) + "***@" + parts[1];
    }
}
