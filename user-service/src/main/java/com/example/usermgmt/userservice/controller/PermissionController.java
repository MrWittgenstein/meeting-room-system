package com.example.usermgmt.userservice.controller;

import com.example.usermgmt.common.api.ApiResponse;
import com.example.usermgmt.userservice.context.CurrentUserContext;
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
import com.example.usermgmt.userservice.service.PermissionManagementService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal")
public class PermissionController {

    private static final String PERMISSION_LIST = "menu:permission:list";
    private static final String PERMISSION_CREATE = "menu:permission:create";
    private static final String PERMISSION_UPDATE = "menu:permission:update";
    private static final String PERMISSION_DELETE = "menu:permission:delete";
    private static final String PERMISSION_GRANT = "menu:permission:grant";

    private final PermissionManagementService permissionManagementService;

    @PostMapping("/permissions")
    public ApiResponse<PermissionResponse> createPermission(@RequestBody PermissionCreateRequest request) {
        CurrentUserContext.requireMenuPermission(PERMISSION_CREATE);
        return ApiResponse.success("created", permissionManagementService.createPermission(request, CurrentUserContext.requireUserId()));
    }

    @PutMapping("/permissions/{permId}")
    public ApiResponse<PermissionResponse> updatePermission(@PathVariable("permId") String permId, @RequestBody PermissionUpdateRequest request) {
        CurrentUserContext.requireMenuPermission(PERMISSION_UPDATE);
        return ApiResponse.success("updated", permissionManagementService.updatePermission(permId, request));
    }

    @DeleteMapping("/permissions/{permId}")
    public ApiResponse<Void> deletePermission(@PathVariable("permId") String permId, @RequestParam(value = "force", defaultValue = "false") boolean force) {
        CurrentUserContext.requireMenuPermission(PERMISSION_DELETE);
        permissionManagementService.deletePermission(permId, force);
        return ApiResponse.success("deleted", null);
    }

    @GetMapping("/permissions")
    public ApiResponse<List<PermissionNodeResponse>> listPermissions(@RequestParam(value = "keyword", required = false) String keyword) {
        CurrentUserContext.requireMenuPermission(PERMISSION_LIST);
        PermissionQueryRequest request = new PermissionQueryRequest();
        request.setKeyword(keyword);
        return ApiResponse.success(permissionManagementService.listPermissions(request));
    }

    @PostMapping("/groups/{groupId}/permissions")
    public ApiResponse<Void> grantGroupPermissions(@PathVariable("groupId") String groupId, @RequestBody GrantPermissionRequest request) {
        CurrentUserContext.requireMenuPermission(PERMISSION_GRANT);
        permissionManagementService.grantGroupPermissions(groupId, request, CurrentUserContext.requireUserId());
        return ApiResponse.success("granted", null);
    }

    @DeleteMapping("/groups/{groupId}/permissions")
    public ApiResponse<Void> revokeGroupPermissions(@PathVariable("groupId") String groupId, @RequestBody GrantPermissionRequest request) {
        CurrentUserContext.requireMenuPermission(PERMISSION_GRANT);
        permissionManagementService.revokeGroupPermissions(groupId, request);
        return ApiResponse.success("revoked", null);
    }

    @GetMapping("/groups/{groupId}/permissions")
    public ApiResponse<List<PermissionNodeResponse>> getGroupPermissions(@PathVariable("groupId") String groupId) {
        CurrentUserContext.requireMenuPermission(PERMISSION_GRANT);
        return ApiResponse.success(permissionManagementService.getGroupPermissions(groupId));
    }

    @PostMapping("/users/{userId}/permissions")
    public ApiResponse<Void> grantUserPermissions(@PathVariable("userId") String userId, @RequestBody GrantPermissionRequest request) {
        CurrentUserContext.requireMenuPermission(PERMISSION_GRANT);
        permissionManagementService.grantUserPermissions(userId, request, CurrentUserContext.requireUserId());
        return ApiResponse.success("granted", null);
    }

    @DeleteMapping("/users/{userId}/permissions")
    public ApiResponse<Void> revokeUserPermissions(@PathVariable("userId") String userId, @RequestBody GrantPermissionRequest request) {
        CurrentUserContext.requireMenuPermission(PERMISSION_GRANT);
        permissionManagementService.revokeUserPermissions(userId, request);
        return ApiResponse.success("revoked", null);
    }

    @GetMapping("/users/{userId}/permissions")
    public ApiResponse<List<PermissionNodeResponse>> getUserDirectPermissions(@PathVariable("userId") String userId) {
        CurrentUserContext.requireMenuPermission(PERMISSION_GRANT);
        return ApiResponse.success(permissionManagementService.getUserDirectPermissions(userId));
    }

    @GetMapping("/users/{userId}/permissions/effective")
    public ApiResponse<UserEffectivePermissionsResponse> getEffectivePermissions(@PathVariable("userId") String userId) {
        if (!userId.equals(CurrentUserContext.requireUserId())) {
            CurrentUserContext.requireMenuPermission(PERMISSION_GRANT);
        }
        return ApiResponse.success(permissionManagementService.getEffectivePermissions(userId));
    }

    @GetMapping("/users/{userId}/permissions/source/{permId}")
    public ApiResponse<PermissionSourceResponse> getPermissionSource(@PathVariable("userId") String userId, @PathVariable("permId") String permId) {
        CurrentUserContext.requireMenuPermission(PERMISSION_GRANT);
        return ApiResponse.success(permissionManagementService.getPermissionSource(userId, permId));
    }

    @GetMapping("/permissions/{permId}/groups")
    public ApiResponse<PermissionHolderGroupsResponse> getPermissionGroups(
        @PathVariable("permId") String permId,
        @RequestParam(value = "page", defaultValue = "1") Integer page,
        @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", defaultValue = "active") String status
    ) {
        CurrentUserContext.requireMenuPermission(PERMISSION_GRANT);
        return ApiResponse.success(permissionManagementService.getPermissionGroups(permId, page, pageSize, keyword, status));
    }

    @GetMapping("/permissions/{permId}/users")
    public ApiResponse<PermissionHolderUsersResponse> getPermissionUsers(
        @PathVariable("permId") String permId,
        @RequestParam(value = "page", defaultValue = "1") Integer page,
        @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "sourceType", defaultValue = "all") String sourceType,
        @RequestParam(value = "status", defaultValue = "active") String status
    ) {
        CurrentUserContext.requireMenuPermission(PERMISSION_GRANT);
        return ApiResponse.success(permissionManagementService.getPermissionUsers(permId, page, pageSize, keyword, sourceType, status));
    }
}
