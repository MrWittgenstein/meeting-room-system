package com.example.usermgmt.userservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.usermgmt.common.api.PageResponse;
import com.example.usermgmt.common.enums.UserStatus;
import com.example.usermgmt.common.exception.BusinessException;
import com.example.usermgmt.userservice.dto.auth.GroupAttributeExtraData;
import com.example.usermgmt.userservice.dto.auth.GroupExtraData;
import com.example.usermgmt.userservice.dto.common.BatchOperationRequest;
import com.example.usermgmt.userservice.dto.common.BatchFailItemResponse;
import com.example.usermgmt.userservice.dto.common.BatchOperationResponse;
import com.example.usermgmt.userservice.dto.group.GroupAttributeRequest;
import com.example.usermgmt.userservice.dto.group.GroupAttributeResponse;
import com.example.usermgmt.userservice.dto.group.GroupCreateRequest;
import com.example.usermgmt.userservice.dto.group.GroupJoinRequest;
import com.example.usermgmt.userservice.dto.group.GroupMemberOperateRequest;
import com.example.usermgmt.userservice.dto.group.GroupMemberResponse;
import com.example.usermgmt.userservice.dto.group.GroupMemberUpgradeRequest;
import com.example.usermgmt.userservice.dto.group.GroupQueryRequest;
import com.example.usermgmt.userservice.dto.group.GroupResponse;
import com.example.usermgmt.userservice.dto.group.GroupUpdateRequest;
import com.example.usermgmt.userservice.entity.AttributeDataEntity;
import com.example.usermgmt.userservice.entity.GroupEntity;
import com.example.usermgmt.userservice.entity.PermissionGroupEntity;
import com.example.usermgmt.userservice.entity.UserGroupEntity;
import com.example.usermgmt.userservice.entity.UserEntity;
import com.example.usermgmt.userservice.mapper.AttributeDataMapper;
import com.example.usermgmt.userservice.mapper.GroupMapper;
import com.example.usermgmt.userservice.mapper.PermissionGroupMapper;
import com.example.usermgmt.userservice.mapper.PermissionMapper;
import com.example.usermgmt.userservice.mapper.UserGroupMapper;
import com.example.usermgmt.userservice.mapper.UserMapper;
import com.example.usermgmt.userservice.service.GroupManagementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class GroupManagementServiceImpl implements GroupManagementService {

    private final AttributeDataMapper attributeDataMapper;
    private final GroupMapper groupMapper;
    private final PermissionGroupMapper permissionGroupMapper;
    private final UserMapper userMapper;
    private final UserGroupMapper userGroupMapper;
    private final PermissionMapper permissionMapper;
    private final ObjectMapper objectMapper;

    @Override
    public GroupResponse createGroup(GroupCreateRequest request, String operatorId) {
        GroupEntity entity = new GroupEntity();
        entity.setId(generateId());
        entity.setName(request.getGroupName());
        entity.setType("custom");
        entity.setStatus(1);
        entity.setDescription(request.getDescription());
        entity.setCreatorId(operatorId);
        entity.setParentId(null);
        entity.setExtraData(UserExtraDataHelper.toJson(objectMapper, buildGroupExtraData(request.getTempPassword(), request.getTempPasswordExpireHours())));
        groupMapper.insert(entity);

        if (request.getInitialMembers() != null && !request.getInitialMembers().isEmpty()) {
            GroupMemberOperateRequest memberRequest = new GroupMemberOperateRequest();
            memberRequest.setUserIds(request.getInitialMembers());
            memberRequest.setRole("member");
            addMembers(entity.getId(), memberRequest, operatorId);
        }
        return getGroupDetail(entity.getId());
    }

    @Override
    public PageResponse<GroupResponse> listGroups(GroupQueryRequest request) {
        return doListGroups(request, false, null);
    }

    @Override
    public PageResponse<GroupResponse> listMyGroups(String userId, GroupQueryRequest request) {
        PageResponse<GroupResponse> pageResponse = doListGroups(request, false, userId);
        List<GroupResponse> filtered = pageResponse.getList().stream()
            .map(item -> {
                GroupResponse detail = getGroupDetail(item.getGroupId());
                UserGroupEntity membership = findMembership(userId, item.getGroupId());
                detail.setMyRole(membership == null ? null : membership.getType());
                detail.setTempExpireAt(parseMemberExpireAt(membership));
                return detail;
            })
            .toList();
        pageResponse.setList(filtered);
        return pageResponse;
    }

    @Override
    public PageResponse<GroupResponse> listPublicGroups(GroupQueryRequest request) {
        PageResponse<GroupResponse> pageResponse = doListGroups(request, true, null);
        List<GroupResponse> filtered = pageResponse.getList().stream()
            .map(item -> {
                GroupResponse publicView = new GroupResponse();
                publicView.setGroupId(item.getGroupId());
                publicView.setGroupName(item.getGroupName());
                publicView.setDescription(item.getDescription());
                publicView.setHasTempPassword(item.getHasTempPassword());
                publicView.setTempPasswordExpireAt(item.getTempPasswordExpireAt());
                return publicView;
            })
            .toList();
        pageResponse.setList(filtered);
        return pageResponse;
    }

    @Override
    public GroupResponse getGroupDetail(String groupId) {
        GroupEntity group = requireGroup(groupId);
        return fillGroupDetail(group);
    }

    @Override
    public GroupResponse getMyGroupDetail(String groupId, String userId) {
        if (!isMember(userId, groupId)) {
            throw new BusinessException(40301, "无权限操作");
        }
        GroupResponse response = getGroupDetail(groupId);
        UserGroupEntity membership = findMembership(userId, groupId);
        response.setMyRole(membership == null ? null : membership.getType());
        response.setTempExpireAt(parseMemberExpireAt(membership));
        return response;
    }

    @Override
    public BatchOperationResponse updateGroups(GroupUpdateRequest request, String operatorId) {
        if (request.getGroupIds() == null || request.getGroupIds().isEmpty()) {
            throw new BusinessException(40001, "组织ID不能为空");
        }
        BatchOperationResponse response = new BatchOperationResponse();
        List<BatchFailItemResponse> failList = new ArrayList<>();
        int success = 0;
        for (String groupId : request.getGroupIds()) {
            GroupEntity group = groupMapper.selectById(groupId);
            if (group == null) {
                failList.add(BatchFailItemResponse.group(groupId, "组织不存在"));
                continue;
            }
            UpdateWrapper<GroupEntity> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", groupId);
            if (StringUtils.hasText(request.getGroupName())) {
                updateWrapper.set("name", request.getGroupName());
            }
            if (StringUtils.hasText(request.getDescription())) {
                updateWrapper.set("description", request.getDescription());
            }
            if (request.getStatus() != null) {
                updateWrapper.set("status", request.getStatus());
            }
            updateWrapper.set("creator_id", operatorId);
            groupMapper.update(null, updateWrapper);
            success++;
        }
        response.setSuccessCount(success);
        response.setFailCount(failList.size());
        response.setFailList(failList);
        return response;
    }

    @Override
    public BatchOperationResponse addMembers(String groupId, GroupMemberOperateRequest request, String operatorId) {
        requireGroup(groupId);
        String role = StringUtils.hasText(request.getRole()) ? request.getRole() : "member";
        BatchOperationResponse response = new BatchOperationResponse();
        List<BatchFailItemResponse> failList = new ArrayList<>();
        int success = 0;
        for (String userId : request.getUserIds()) {
            UserEntity user = userMapper.selectById(userId);
            if (user == null) {
                failList.add(BatchFailItemResponse.user(userId, "用户不存在"));
                continue;
            }
            if (user.getStatus() == null || user.getStatus() != UserStatus.ACTIVE.getCode()) {
                failList.add(BatchFailItemResponse.user(userId, "用户已注销或不可用"));
                continue;
            }
            if (isMember(userId, groupId)) {
                failList.add(BatchFailItemResponse.user(userId, "用户已是该组织成员"));
                continue;
            }
            UserGroupEntity relation = new UserGroupEntity();
            relation.setUserId(userId);
            relation.setGroupId(groupId);
            relation.setCreatorId(operatorId);
            relation.setLastOperatorId(operatorId);
            relation.setType(role);
            userGroupMapper.insert(relation);
            success++;
        }
        response.setSuccessCount(success);
        response.setFailCount(failList.size());
        response.setFailList(failList);
        return response;
    }

    @Override
    public BatchOperationResponse removeMembers(String groupId, GroupMemberOperateRequest request) {
        requireGroup(groupId);
        BatchOperationResponse response = new BatchOperationResponse();
        List<BatchFailItemResponse> failList = new ArrayList<>();
        int success = 0;
        for (String userId : request.getUserIds()) {
            int affected = userGroupMapper.delete(new QueryWrapper<UserGroupEntity>().eq("user_id", userId).eq("group_id", groupId));
            if (affected == 0) {
                failList.add(BatchFailItemResponse.user(userId, "用户不是该组织成员"));
                continue;
            }
            success++;
        }
        response.setSuccessCount(success);
        response.setFailCount(failList.size());
        response.setFailList(failList);
        return response;
    }

    @Override
    public GroupResponse setTempPassword(String groupId, String tempPassword, Integer expireHours) {
        GroupEntity group = requireGroup(groupId);
        group.setExtraData(UserExtraDataHelper.toJson(objectMapper, buildGroupExtraData(tempPassword, expireHours)));
        groupMapper.updateById(group);
        return getGroupDetail(groupId);
    }

    @Override
    public void clearTempPassword(String groupId) {
        GroupEntity group = requireGroup(groupId);
        group.setExtraData(UserExtraDataHelper.toJson(objectMapper, new GroupExtraData()));
        groupMapper.updateById(group);
    }

    @Override
    public GroupResponse joinGroup(String groupId, String userId, GroupJoinRequest request) {
        GroupEntity group = requireGroup(groupId);
        if (isMember(userId, groupId)) {
            throw new BusinessException(42004, "用户已是组织成员");
        }
        GroupExtraData extraData = UserExtraDataHelper.parse(objectMapper, group.getExtraData(), GroupExtraData.class);
        if (extraData == null || !StringUtils.hasText(extraData.getTempPassword())) {
            throw new BusinessException(42001, "组织不存在或已禁用");
        }
        if (!extraData.getTempPassword().equals(request.getTempPassword())) {
            throw new BusinessException(42002, "临时密码错误");
        }
        if (extraData.getTempPasswordExpireAt() != null && extraData.getTempPasswordExpireAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(42003, "临时密码已过期");
        }

        UserGroupEntity relation = new UserGroupEntity();
        relation.setUserId(userId);
        relation.setGroupId(groupId);
        relation.setType("temporary");
        relation.setExtension(extraData.getTempPasswordExpireAt() == null ? null : extraData.getTempPasswordExpireAt().toString());
        userGroupMapper.insert(relation);
        return getGroupDetail(groupId);
    }

    @Override
    public GroupResponse upgradeMember(String groupId, String userId, GroupMemberUpgradeRequest request, String operatorId) {
        QueryWrapper<UserGroupEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId).eq("group_id", groupId);
        UserGroupEntity relation = userGroupMapper.selectOne(queryWrapper);
        if (relation == null) {
            throw new BusinessException(404, "组织成员不存在");
        }
        UpdateWrapper<UserGroupEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("user_id", userId)
            .eq("group_id", groupId)
            .set("type", StringUtils.hasText(request.getRole()) ? request.getRole() : "member")
            .set("extension", null)
            .set("last_operator_id", operatorId);
        userGroupMapper.update(null, updateWrapper);
        return getGroupDetail(groupId);
    }

    @Override
    public BatchOperationResponse deleteGroups(BatchOperationRequest request) {
        BatchOperationResponse response = new BatchOperationResponse();
        List<BatchFailItemResponse> failList = new ArrayList<>();
        int success = 0;
        for (String groupId : request.getIds()) {
            GroupEntity group = groupMapper.selectById(groupId);
            if (group == null) {
                failList.add(BatchFailItemResponse.group(groupId, "组织不存在"));
                continue;
            }
            int memberCount = userGroupMapper.countMembers(groupId);
            boolean force = Boolean.TRUE.equals(request.getForce());
            if (!force && memberCount > 0) {
                failList.add(BatchFailItemResponse.group(groupId, "组织非空，请先移除所有成员或使用强制删除"));
                continue;
            }
            userGroupMapper.delete(new QueryWrapper<UserGroupEntity>().eq("group_id", groupId));
            permissionGroupMapper.delete(new QueryWrapper<PermissionGroupEntity>().eq("group_id", groupId));
            attributeDataMapper.delete(new QueryWrapper<AttributeDataEntity>().eq("group_id", groupId));
            groupMapper.deleteById(groupId);
            success++;
        }
        response.setSuccessCount(success);
        response.setFailCount(failList.size());
        response.setFailList(failList);
        return response;
    }

    @Override
    public GroupAttributeResponse createGroupAttribute(String groupId, GroupAttributeRequest request) {
        requireGroup(groupId);
        validateAttributeRequest(request, null, groupId);
        AttributeDataEntity entity = new AttributeDataEntity();
        entity.setId(generateId());
        entity.setGroupId(groupId);
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setExtraData(toAttributeExtraJson(request));
        attributeDataMapper.insert(entity);
        return toGroupAttributeResponse(entity);
    }

    @Override
    public GroupAttributeResponse updateGroupAttribute(String groupId, String attributeId, GroupAttributeRequest request) {
        requireGroup(groupId);
        AttributeDataEntity entity = requireGroupAttribute(groupId, attributeId);
        validateAttributeRequest(request, attributeId, groupId);
        if (StringUtils.hasText(request.getName())) {
            entity.setName(request.getName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        GroupAttributeResponse current = toGroupAttributeResponse(entity);
        GroupAttributeExtraData extraData = new GroupAttributeExtraData();
        extraData.setFieldKey(StringUtils.hasText(request.getFieldKey()) ? request.getFieldKey() : current.getFieldKey());
        extraData.setFieldType(StringUtils.hasText(request.getFieldType()) ? request.getFieldType() : current.getFieldType());
        extraData.setRequired(request.getRequired() != null ? request.getRequired() : current.getRequired());
        extraData.setValue(request.getValue() != null ? request.getValue() : current.getValue());
        extraData.setDefaultValue(request.getDefaultValue() != null ? request.getDefaultValue() : current.getDefaultValue());
        extraData.setOptions(request.getOptions() != null ? request.getOptions() : current.getOptions());
        extraData.setSort(request.getSort() != null ? request.getSort() : current.getSort());
        entity.setExtraData(UserExtraDataHelper.toJson(objectMapper, extraData));
        attributeDataMapper.updateById(entity);
        return toGroupAttributeResponse(entity);
    }

    @Override
    public void deleteGroupAttribute(String groupId, String attributeId) {
        requireGroupAttribute(groupId, attributeId);
        attributeDataMapper.deleteById(attributeId);
    }

    @Override
    public List<GroupAttributeResponse> listGroupAttributes(String groupId) {
        requireGroup(groupId);
        return attributeDataMapper.selectGroupAttributes(groupId).stream()
            .map(this::toGroupAttributeResponse)
            .sorted(Comparator.comparing(item -> item.getSort() == null ? Integer.MAX_VALUE : item.getSort()))
            .toList();
    }

    private PageResponse<GroupResponse> doListGroups(GroupQueryRequest request, boolean onlyTemp, String userId) {
        int page = request.getPage() == null || request.getPage() < 1 ? 1 : request.getPage();
        int pageSize = request.getPageSize() == null || request.getPageSize() < 1 ? 20 : Math.min(request.getPageSize(), 100);
        List<GroupEntity> groups = groupMapper.selectGroups(request.getKeyword(), request.getStatus(), 0, Integer.MAX_VALUE);
        List<GroupResponse> filtered = groups.stream()
            .map(this::toSummaryResponse)
            .filter(item -> !onlyTemp || (Boolean.TRUE.equals(item.getHasTempPassword()) && item.getTempPasswordExpireAt() != null && item.getTempPasswordExpireAt().isAfter(LocalDateTime.now())))
            .filter(item -> userId == null || isMember(userId, item.getGroupId()))
            .toList();
        int fromIndex = Math.min((page - 1) * pageSize, filtered.size());
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        return PageResponse.<GroupResponse>builder()
            .total((long) filtered.size())
            .page(page)
            .pageSize(pageSize)
            .list(filtered.subList(fromIndex, toIndex))
            .build();
    }

    private GroupResponse fillGroupDetail(GroupEntity group) {
        GroupResponse response = toSummaryResponse(group);
        List<GroupMemberResponse> members = new ArrayList<>();
        for (UserGroupMapper.UserGroupMemberRow row : userGroupMapper.selectGroupMembers(group.getId())) {
            GroupMemberResponse member = new GroupMemberResponse();
            member.setUserId(row.userId);
            member.setNickname(row.userName);
            member.setAvatar(row.avatar);
            member.setRole(row.type);
            member.setJoinedAt(row.createdTime);
            members.add(member);
        }
        response.setMembers(members);
        response.setMemberNames(members.stream().map(GroupMemberResponse::getNickname).filter(StringUtils::hasText).toList());
        response.setPermissions(permissionMapper.selectGroupPermissions(group.getId()));
        response.setAttributes(listGroupAttributes(group.getId()));
        return response;
    }

    private boolean isMember(String userId, String groupId) {
        return userGroupMapper.selectCount(new QueryWrapper<UserGroupEntity>().eq("user_id", userId).eq("group_id", groupId)) > 0;
    }

    private UserGroupEntity findMembership(String userId, String groupId) {
        return userGroupMapper.selectOne(new QueryWrapper<UserGroupEntity>().eq("user_id", userId).eq("group_id", groupId));
    }

    private LocalDateTime parseMemberExpireAt(UserGroupEntity membership) {
        if (membership == null || !StringUtils.hasText(membership.getExtension())) {
            return null;
        }
        return LocalDateTime.parse(membership.getExtension());
    }

    private GroupEntity requireGroup(String groupId) {
        GroupEntity entity = groupMapper.selectById(groupId);
        if (entity == null) {
            throw new BusinessException(40001, "组织不存在");
        }
        return entity;
    }

    private AttributeDataEntity requireGroupAttribute(String groupId, String attributeId) {
        AttributeDataEntity entity = attributeDataMapper.selectById(attributeId);
        if (entity == null || !groupId.equals(entity.getGroupId())) {
            throw new BusinessException(404, "组织属性不存在");
        }
        return entity;
    }

    private void validateAttributeRequest(GroupAttributeRequest request, String attributeId, String groupId) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BusinessException(400, "属性名称不能为空");
        }
        if (!StringUtils.hasText(request.getFieldKey())) {
            throw new BusinessException(400, "属性键不能为空");
        }
        List<AttributeDataEntity> existed = attributeDataMapper.selectGroupAttributes(groupId);
        for (AttributeDataEntity existedEntity : existed) {
            if (attributeId != null && attributeId.equals(existedEntity.getId())) {
                continue;
            }
            GroupAttributeResponse existedResponse = toGroupAttributeResponse(existedEntity);
            if (request.getFieldKey().equals(existedResponse.getFieldKey())) {
                throw new BusinessException(40001, "属性键已存在");
            }
        }
    }

    private String toAttributeExtraJson(GroupAttributeRequest request) {
        GroupAttributeExtraData extraData = new GroupAttributeExtraData();
        extraData.setFieldKey(request.getFieldKey());
        extraData.setFieldType(StringUtils.hasText(request.getFieldType()) ? request.getFieldType() : "string");
        extraData.setRequired(Boolean.TRUE.equals(request.getRequired()));
        extraData.setValue(request.getValue());
        extraData.setDefaultValue(request.getDefaultValue());
        extraData.setOptions(request.getOptions());
        extraData.setSort(request.getSort() == null ? 0 : request.getSort());
        return UserExtraDataHelper.toJson(objectMapper, extraData);
    }

    private GroupAttributeResponse toGroupAttributeResponse(AttributeDataEntity entity) {
        GroupAttributeExtraData extraData = UserExtraDataHelper.parse(objectMapper, entity.getExtraData(), GroupAttributeExtraData.class);
        GroupAttributeResponse response = new GroupAttributeResponse();
        response.setAttributeId(entity.getId());
        response.setGroupId(entity.getGroupId());
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

    private GroupResponse toSummaryResponse(GroupEntity group) {
        GroupExtraData extraData = UserExtraDataHelper.parse(objectMapper, group.getExtraData(), GroupExtraData.class);
        GroupResponse response = new GroupResponse();
        response.setGroupId(group.getId());
        response.setGroupName(group.getName());
        response.setDescription(group.getDescription());
        response.setStatus(toGroupStatus(group.getStatus()));
        response.setMemberCount(userGroupMapper.countMembers(group.getId()));
        response.setMemberNames(
            userGroupMapper.selectGroupMembers(group.getId()).stream()
                .map(row -> row.userName)
                .filter(StringUtils::hasText)
                .toList()
        );
        response.setHasTempPassword(extraData != null && StringUtils.hasText(extraData.getTempPassword()));
        response.setTempPasswordExpireAt(extraData == null ? null : extraData.getTempPasswordExpireAt());
        response.setCreatedAt(group.getCreateTime());
        response.setUpdatedAt(group.getLastUpdateTime());
        return response;
    }

    private GroupExtraData buildGroupExtraData(String tempPassword, Integer expireHours) {
        GroupExtraData extraData = new GroupExtraData();
        if (StringUtils.hasText(tempPassword)) {
            extraData.setTempPassword(tempPassword);
            extraData.setTempPasswordExpireAt(LocalDateTime.now().plusHours(expireHours == null ? 24 : expireHours));
        }
        return extraData;
    }

    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String toGroupStatus(Integer status) {
        return status != null && status == 1 ? "active" : "disabled";
    }
}
