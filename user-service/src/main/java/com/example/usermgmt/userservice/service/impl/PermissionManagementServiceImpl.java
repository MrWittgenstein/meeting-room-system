package com.example.usermgmt.userservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.usermgmt.common.exception.BusinessException;
import com.example.usermgmt.userservice.dto.group.PermissionNodeResponse;
import com.example.usermgmt.userservice.dto.permission.GrantPermissionRequest;
import com.example.usermgmt.userservice.dto.permission.PermissionCreateRequest;
import com.example.usermgmt.userservice.dto.permission.PermissionHolderGroupsResponse;
import com.example.usermgmt.userservice.dto.permission.PermissionHolderUsersResponse;
import com.example.usermgmt.userservice.dto.permission.PermissionQueryRequest;
import com.example.usermgmt.userservice.dto.permission.PermissionResponse;
import com.example.usermgmt.userservice.dto.permission.PermissionSourceResponse;
import com.example.usermgmt.userservice.dto.permission.PermissionUpdateRequest;
import com.example.usermgmt.userservice.dto.permission.UserEffectivePermissionsResponse;
import com.example.usermgmt.userservice.entity.PermissionEntity;
import com.example.usermgmt.userservice.entity.PermissionGroupEntity;
import com.example.usermgmt.userservice.entity.PermissionUserEntity;
import com.example.usermgmt.userservice.entity.UserEntity;
import com.example.usermgmt.userservice.mapper.GroupMapper;
import com.example.usermgmt.userservice.mapper.PermissionGroupMapper;
import com.example.usermgmt.userservice.mapper.PermissionMapper;
import com.example.usermgmt.userservice.mapper.PermissionUserMapper;
import com.example.usermgmt.userservice.mapper.UserGroupMapper;
import com.example.usermgmt.userservice.mapper.UserMapper;
import com.example.usermgmt.userservice.service.PermissionManagementService;
import com.example.usermgmt.userservice.support.BuiltInGroupConstants;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PermissionManagementServiceImpl implements PermissionManagementService {

    private final PermissionMapper permissionMapper;
    private final PermissionUserMapper permissionUserMapper;
    private final PermissionGroupMapper permissionGroupMapper;
    private final GroupMapper groupMapper;
    private final UserMapper userMapper;
    private final UserGroupMapper userGroupMapper;

    @Override
    public PermissionResponse createPermission(PermissionCreateRequest request, String operatorId) {
        if (permissionMapper.selectCount(new QueryWrapper<PermissionEntity>().eq("code", request.getPermCode())) > 0) {
            throw new BusinessException(40001, "权限代码已存在");
        }
        PermissionEntity entity = new PermissionEntity();
        entity.setId(generateId());
        entity.setCode(request.getPermCode());
        entity.setName(request.getPermName());
        entity.setParentId(request.getParentId());
        entity.setPath(request.getPath());
        entity.setIcon(request.getIcon());
        entity.setSort(request.getSort() == null ? 0 : request.getSort());
        entity.setStatus(1);
        entity.setDescription(request.getDescription());
        entity.setCreatorId(operatorId);
        entity.setMethodType(StringUtils.hasText(request.getMethodType()) ? request.getMethodType() : "menu");
        permissionMapper.insert(entity);
        grantPermissionToAdminGroup(entity.getId(), operatorId);
        return toPermissionResponse(entity);
    }

    @Override
    public PermissionResponse updatePermission(String permId, PermissionUpdateRequest request) {
        PermissionEntity entity = requirePermission(permId);
        if (StringUtils.hasText(request.getPermName())) {
            entity.setName(request.getPermName());
        }
        if (request.getParentId() != null) {
            entity.setParentId(request.getParentId());
        }
        if (request.getPath() != null) {
            entity.setPath(request.getPath());
        }
        if (request.getIcon() != null) {
            entity.setIcon(request.getIcon());
        }
        if (request.getSort() != null) {
            entity.setSort(request.getSort());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        permissionMapper.updateById(entity);
        return toPermissionResponse(entity);
    }

    @Override
    public void deletePermission(String permId, boolean force) {
        requirePermission(permId);
        List<PermissionEntity> children = permissionMapper.selectList(new QueryWrapper<PermissionEntity>().eq("parent_id", permId));
        if (!force && !children.isEmpty()) {
            throw new BusinessException(40002, "权限存在子权限，请先删除子权限或使用强制删除");
        }
        if (permissionUserMapper.selectCount(new QueryWrapper<PermissionUserEntity>().eq("permission_id", permId)) > 0
            || permissionGroupMapper.selectCount(new QueryWrapper<PermissionGroupEntity>().eq("permission_id", permId)) > 0) {
            throw new BusinessException(40003, "权限已被分配，无法删除");
        }
        if (force) {
            for (PermissionEntity child : children) {
                deletePermission(child.getId(), true);
            }
        }
        permissionMapper.deleteById(permId);
    }

    @Override
    public List<PermissionNodeResponse> listPermissions(PermissionQueryRequest request) {
        QueryWrapper<PermissionEntity> queryWrapper = new QueryWrapper<>();
        if (StringUtils.hasText(request.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper.like("name", request.getKeyword()).or().like("code", request.getKeyword()));
        }
        queryWrapper.orderByAsc("sort").orderByAsc("create_time");
        List<PermissionEntity> entities = permissionMapper.selectList(queryWrapper);
        return buildTree(entities.stream().map(this::toPermissionNode).toList());
    }

    @Override
    public void grantGroupPermissions(String groupId, GrantPermissionRequest request, String operatorId) {
        requireGroup(groupId);
        for (String permId : request.getPermIds()) {
            requirePermission(permId);
            QueryWrapper<PermissionGroupEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("group_id", groupId).eq("permission_id", permId);
            if (permissionGroupMapper.selectCount(queryWrapper) > 0) {
                continue;
            }
            PermissionGroupEntity relation = new PermissionGroupEntity();
            relation.setGroupId(groupId);
            relation.setPermissionId(permId);
            relation.setCreatorId(operatorId);
            permissionGroupMapper.insert(relation);
        }
    }

    private void grantPermissionToAdminGroup(String permissionId, String operatorId) {
        QueryWrapper<PermissionGroupEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("group_id", BuiltInGroupConstants.ADMIN_GROUP_ID).eq("permission_id", permissionId);
        if (permissionGroupMapper.selectCount(queryWrapper) > 0) {
            return;
        }
        PermissionGroupEntity relation = new PermissionGroupEntity();
        relation.setGroupId(BuiltInGroupConstants.ADMIN_GROUP_ID);
        relation.setPermissionId(permissionId);
        relation.setCreatorId(StringUtils.hasText(operatorId) ? operatorId : "system");
        relation.setDescription("Admin group default all active permissions");
        permissionGroupMapper.insert(relation);
    }

    @Override
    public void revokeGroupPermissions(String groupId, GrantPermissionRequest request) {
        for (String permId : request.getPermIds()) {
            permissionGroupMapper.delete(new QueryWrapper<PermissionGroupEntity>().eq("group_id", groupId).eq("permission_id", permId));
        }
    }

    @Override
    public List<PermissionNodeResponse> getGroupPermissions(String groupId) {
        requireGroup(groupId);
        return buildTree(permissionMapper.selectGroupPermissions(groupId));
    }

    @Override
    public void grantUserPermissions(String userId, GrantPermissionRequest request, String operatorId) {
        requireUser(userId);
        if (Boolean.TRUE.equals(request.getOverwrite())) {
            permissionUserMapper.delete(new QueryWrapper<PermissionUserEntity>().eq("user_id", userId));
        }
        for (String permId : request.getPermIds()) {
            requirePermission(permId);
            QueryWrapper<PermissionUserEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", userId).eq("permission_id", permId);
            if (permissionUserMapper.selectCount(queryWrapper) > 0) {
                continue;
            }
            PermissionUserEntity relation = new PermissionUserEntity();
            relation.setUserId(userId);
            relation.setPermissionId(permId);
            relation.setCreatorId(operatorId);
            permissionUserMapper.insert(relation);
        }
    }

    @Override
    public void revokeUserPermissions(String userId, GrantPermissionRequest request) {
        for (String permId : request.getPermIds()) {
            permissionUserMapper.delete(new QueryWrapper<PermissionUserEntity>().eq("user_id", userId).eq("permission_id", permId));
        }
    }

    @Override
    public List<PermissionNodeResponse> getUserDirectPermissions(String userId) {
        requireUser(userId);
        return buildTree(permissionMapper.selectDirectPermissions(userId));
    }

    @Override
    public UserEffectivePermissionsResponse getEffectivePermissions(String userId) {
        UserEntity user = requireUser(userId);
        UserEffectivePermissionsResponse response = new UserEffectivePermissionsResponse();
        response.setUserId(userId);
        response.setNickname(user.getName());
        List<UserEffectivePermissionsResponse.UserGroupSummary> groups = new ArrayList<>();
        for (UserGroupMapper.UserGroupSimpleRow row : userGroupMapper.selectUserGroups(userId)) {
            UserEffectivePermissionsResponse.UserGroupSummary summary = new UserEffectivePermissionsResponse.UserGroupSummary();
            summary.setGroupId(row.id);
            summary.setGroupName(row.name);
            groups.add(summary);
        }
        response.setGroups(groups);
        response.setPermissions(buildTree(permissionMapper.selectEffectivePermissions(userId)));
        return response;
    }

    @Override
    public PermissionSourceResponse getPermissionSource(String userId, String permId) {
        PermissionEntity permission = requirePermission(permId);
        List<PermissionNodeResponse> effective = permissionMapper.selectEffectivePermissions(userId);
        PermissionSourceResponse response = new PermissionSourceResponse();
        response.setUserId(userId);
        response.setPermId(permId);
        response.setPermCode(permission.getCode());
        response.setPermName(permission.getName());
        List<PermissionSourceResponse.PermissionSourceItem> sources = new ArrayList<>();
        for (PermissionNodeResponse item : effective) {
            if (!permId.equals(item.getPermId())) {
                continue;
            }
            PermissionSourceResponse.PermissionSourceItem source = new PermissionSourceResponse.PermissionSourceItem();
            source.setType(item.getGrantType());
            source.setSourceId("group".equals(item.getGrantType()) ? findGroupIdByName(item.getSource()) : null);
            source.setSourceName(item.getSource());
            source.setGrantedAt(item.getGrantedAt());
            sources.add(source);
        }
        response.setHasPermission(!sources.isEmpty());
        response.setSources(sources);
        return response;
    }

    @Override
    public PermissionHolderGroupsResponse getPermissionGroups(String permId, Integer page, Integer pageSize, String keyword, String status) {
        PermissionEntity permission = requirePermission(permId);
        List<PermissionGroupEntity> relations = permissionGroupMapper.selectList(new QueryWrapper<PermissionGroupEntity>().eq("permission_id", permId));
        List<PermissionHolderGroupsResponse.GroupItem> items = new ArrayList<>();
        for (PermissionGroupEntity relation : relations) {
            var group = groupMapper.selectById(relation.getGroupId());
            if (group == null) {
                continue;
            }
            if (keyword != null && !keyword.isBlank() && (group.getName() == null || !group.getName().contains(keyword))) {
                continue;
            }
            if (!matchesGroupStatus(group.getStatus(), status)) {
                continue;
            }
            PermissionHolderGroupsResponse.GroupItem item = new PermissionHolderGroupsResponse.GroupItem();
            item.setGroupId(group.getId());
            item.setGroupName(group.getName());
            item.setDescription(group.getDescription());
            item.setStatus(group.getStatus() != null && group.getStatus() == 1 ? "active" : "disabled");
            item.setMemberCount(userGroupMapper.countMembers(group.getId()));
            item.setGrantedAt(relation.getCreatedTime() == null ? null : relation.getCreatedTime().toString());
            item.setExpireAt(null);
            item.setIsActive(group.getStatus() != null && group.getStatus() == 1);
            items.add(item);
        }
        long total = items.size();
        items = paginate(items, page, pageSize);
        PermissionHolderGroupsResponse response = new PermissionHolderGroupsResponse();
        response.setPermission(toPermissionResponse(permission));
        response.setTotal(total);
        response.setPage(page);
        response.setPageSize(pageSize);
        response.setList(items);
        return response;
    }

    @Override
    public PermissionHolderUsersResponse getPermissionUsers(String permId, Integer page, Integer pageSize, String keyword, String sourceType, String status) {
        PermissionEntity permission = requirePermission(permId);
        List<PermissionHolderUsersResponse.UserItem> items = new ArrayList<>();

        List<PermissionUserEntity> directRelations = permissionUserMapper.selectList(new QueryWrapper<PermissionUserEntity>().eq("permission_id", permId));
        for (PermissionUserEntity relation : directRelations) {
            UserEntity user = userMapper.selectById(relation.getUserId());
            if (user == null) {
                continue;
            }
            if (!matchesKeyword(user.getName(), keyword) || !matchesUserStatus(null, user, status) || !matchesSourceType("direct", sourceType)) {
                continue;
            }
            PermissionHolderUsersResponse.UserItem item = new PermissionHolderUsersResponse.UserItem();
            item.setUserId(user.getId());
            item.setNickname(user.getName());
            item.setEmail(user.getMail());
            item.setAvatar(UserExtraDataHelper.parse(new com.fasterxml.jackson.databind.ObjectMapper(), user.getExtraData()).getAvatar());
            item.setSourceType("direct");
            item.setGrantedBy(relation.getCreatorId());
            item.setGrantedByName(relation.getCreatorId());
            item.setGrantedAt(relation.getCreatedTime() == null ? null : relation.getCreatedTime().toString());
            item.setExpireAt(null);
            item.setIsActive(user.getStatus() != null && user.getStatus() == 1);
            items.add(item);
        }

        List<PermissionGroupEntity> groupRelations = permissionGroupMapper.selectList(new QueryWrapper<PermissionGroupEntity>().eq("permission_id", permId));
        for (PermissionGroupEntity relation : groupRelations) {
            var group = groupMapper.selectById(relation.getGroupId());
            if (group == null) {
                continue;
            }
            List<com.example.usermgmt.userservice.entity.UserGroupEntity> memberships =
                userGroupMapper.selectList(new QueryWrapper<com.example.usermgmt.userservice.entity.UserGroupEntity>().eq("group_id", group.getId()));
            for (com.example.usermgmt.userservice.entity.UserGroupEntity membership : memberships) {
                UserEntity user = userMapper.selectById(membership.getUserId());
                if (user == null) {
                    continue;
                }
                if (!matchesKeyword(user.getName(), keyword) || !matchesUserStatus(membership, user, status) || !matchesSourceType("group", sourceType)) {
                    continue;
                }
                PermissionHolderUsersResponse.UserItem item = new PermissionHolderUsersResponse.UserItem();
                item.setUserId(user.getId());
                item.setNickname(user.getName());
                item.setEmail(user.getMail());
                item.setAvatar(UserExtraDataHelper.parse(new com.fasterxml.jackson.databind.ObjectMapper(), user.getExtraData()).getAvatar());
                item.setSourceType("group");
                PermissionHolderUsersResponse.SourceDetail detail = new PermissionHolderUsersResponse.SourceDetail();
                detail.setGroupId(group.getId());
                detail.setGroupName(group.getName());
                detail.setRole(membership.getType());
                detail.setJoinedAt(membership.getCreatedTime() == null ? null : membership.getCreatedTime().toString());
                item.setSourceDetail(detail);
                item.setGrantedAt(relation.getCreatedTime() == null ? null : relation.getCreatedTime().toString());
                item.setExpireAt(membership.getExtension());
                item.setIsActive(user.getStatus() != null && user.getStatus() == 1);
                items.add(item);
            }
        }

        items = items.stream()
            .distinct()
            .toList();
        long total = items.size();
        items = paginate(items, page, pageSize);

        PermissionHolderUsersResponse response = new PermissionHolderUsersResponse();
        response.setPermission(toPermissionResponse(permission));
        response.setTotal(total);
        response.setPage(page);
        response.setPageSize(pageSize);
        response.setList(items);
        return response;
    }

    private PermissionEntity requirePermission(String permId) {
        PermissionEntity entity = permissionMapper.selectById(permId);
        if (entity == null) {
            throw new BusinessException(40001, "权限不存在");
        }
        return entity;
    }

    private void requireGroup(String groupId) {
        if (groupMapper.selectById(groupId) == null) {
            throw new BusinessException(40001, "组织不存在");
        }
    }

    private UserEntity requireUser(String userId) {
        UserEntity entity = userMapper.selectById(userId);
        if (entity == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return entity;
    }

    private PermissionResponse toPermissionResponse(PermissionEntity entity) {
        PermissionResponse response = new PermissionResponse();
        response.setPermId(entity.getId());
        response.setPermCode(entity.getCode());
        response.setPermName(entity.getName());
        response.setParentId(entity.getParentId());
        response.setPath(entity.getPath());
        response.setIcon(entity.getIcon());
        response.setSort(entity.getSort());
        response.setDescription(entity.getDescription());
        response.setMethodType(entity.getMethodType());
        response.setCreatedAt(entity.getCreateTime());
        response.setUpdatedAt(entity.getLastUpdateTime());
        return response;
    }

    private PermissionNodeResponse toPermissionNode(PermissionEntity entity) {
        PermissionNodeResponse node = new PermissionNodeResponse();
        node.setPermId(entity.getId());
        node.setPermCode(entity.getCode());
        node.setPermName(entity.getName());
        node.setParentId(entity.getParentId());
        node.setPath(entity.getPath());
        node.setIcon(entity.getIcon());
        node.setSort(entity.getSort());
        node.setDescription(entity.getDescription());
        return node;
    }

    private List<PermissionNodeResponse> buildTree(List<PermissionNodeResponse> nodes) {
        Map<String, PermissionNodeResponse> map = new LinkedHashMap<>();
        for (PermissionNodeResponse node : nodes.stream().sorted(Comparator.comparing(PermissionNodeResponse::getSort, Comparator.nullsFirst(Integer::compareTo))).toList()) {
            node.setChildren(new ArrayList<>());
            map.put(node.getPermId(), node);
        }
        List<PermissionNodeResponse> roots = new ArrayList<>();
        for (PermissionNodeResponse node : map.values()) {
            if (node.getParentId() == null || node.getParentId().isBlank() || !map.containsKey(node.getParentId())) {
                roots.add(node);
            } else {
                map.get(node.getParentId()).getChildren().add(node);
            }
        }
        return roots;
    }

    private String findGroupIdByName(String groupName) {
        QueryWrapper<com.example.usermgmt.userservice.entity.GroupEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", groupName).last("LIMIT 1");
        var entity = groupMapper.selectOne(queryWrapper);
        return entity == null ? null : entity.getId();
    }

    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private boolean matchesKeyword(String source, String keyword) {
        return !StringUtils.hasText(keyword) || (source != null && source.contains(keyword));
    }

    private boolean matchesSourceType(String actualSourceType, String expectedSourceType) {
        return !StringUtils.hasText(expectedSourceType) || "all".equalsIgnoreCase(expectedSourceType) || actualSourceType.equalsIgnoreCase(expectedSourceType);
    }

    private boolean matchesGroupStatus(Integer statusValue, String expectedStatus) {
        if (!StringUtils.hasText(expectedStatus) || "all".equalsIgnoreCase(expectedStatus)) {
            return true;
        }
        if ("active".equalsIgnoreCase(expectedStatus)) {
            return statusValue != null && statusValue == 1;
        }
        if ("disabled".equalsIgnoreCase(expectedStatus)) {
            return statusValue != null && statusValue != 1;
        }
        return true;
    }

    private boolean matchesUserStatus(com.example.usermgmt.userservice.entity.UserGroupEntity membership, UserEntity user, String expectedStatus) {
        if (!StringUtils.hasText(expectedStatus) || "all".equalsIgnoreCase(expectedStatus)) {
            return true;
        }
        if ("expired".equalsIgnoreCase(expectedStatus)) {
            return membership != null
                && StringUtils.hasText(membership.getExtension())
                && LocalDateTime.parse(membership.getExtension()).isBefore(LocalDateTime.now());
        }
        return user.getStatus() != null && user.getStatus() == 1
            && (membership == null || !StringUtils.hasText(membership.getExtension()) || LocalDateTime.parse(membership.getExtension()).isAfter(LocalDateTime.now()));
    }

    private <T> List<T> paginate(List<T> items, Integer page, Integer pageSize) {
        int currentPage = page == null || page < 1 ? 1 : page;
        int currentPageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        int fromIndex = Math.min((currentPage - 1) * currentPageSize, items.size());
        int toIndex = Math.min(fromIndex + currentPageSize, items.size());
        return items.subList(fromIndex, toIndex);
    }
}
