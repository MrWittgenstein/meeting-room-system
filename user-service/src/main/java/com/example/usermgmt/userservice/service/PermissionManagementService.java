package com.example.usermgmt.userservice.service;

import com.example.usermgmt.userservice.dto.group.PermissionNodeResponse;
import com.example.usermgmt.userservice.dto.permission.GrantPermissionRequest;
import com.example.usermgmt.userservice.dto.permission.PermissionCreateRequest;
import com.example.usermgmt.userservice.dto.permission.PermissionQueryRequest;
import com.example.usermgmt.userservice.dto.permission.PermissionResponse;
import com.example.usermgmt.userservice.dto.permission.PermissionSourceResponse;
import com.example.usermgmt.userservice.dto.permission.PermissionHolderGroupsResponse;
import com.example.usermgmt.userservice.dto.permission.PermissionHolderUsersResponse;
import com.example.usermgmt.userservice.dto.permission.PermissionUpdateRequest;
import com.example.usermgmt.userservice.dto.permission.UserEffectivePermissionsResponse;
import java.util.List;

public interface PermissionManagementService {

    PermissionResponse createPermission(PermissionCreateRequest request, String operatorId);

    PermissionResponse updatePermission(String permId, PermissionUpdateRequest request);

    void deletePermission(String permId, boolean force);

    List<PermissionNodeResponse> listPermissions(PermissionQueryRequest request);

    void grantGroupPermissions(String groupId, GrantPermissionRequest request, String operatorId);

    void revokeGroupPermissions(String groupId, GrantPermissionRequest request);

    List<PermissionNodeResponse> getGroupPermissions(String groupId);

    void grantUserPermissions(String userId, GrantPermissionRequest request, String operatorId);

    void revokeUserPermissions(String userId, GrantPermissionRequest request);

    List<PermissionNodeResponse> getUserDirectPermissions(String userId);

    UserEffectivePermissionsResponse getEffectivePermissions(String userId);

    PermissionSourceResponse getPermissionSource(String userId, String permId);

    PermissionHolderGroupsResponse getPermissionGroups(String permId, Integer page, Integer pageSize, String keyword, String status);

    PermissionHolderUsersResponse getPermissionUsers(String permId, Integer page, Integer pageSize, String keyword, String sourceType, String status);
}
