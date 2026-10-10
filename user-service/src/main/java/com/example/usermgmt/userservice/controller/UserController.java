package com.example.usermgmt.userservice.controller;

import com.example.usermgmt.common.api.ApiResponse;
import com.example.usermgmt.common.api.PageResponse;
import com.example.usermgmt.userservice.context.CurrentUserContext;
import com.example.usermgmt.userservice.dto.common.BatchOperationResponse;
import com.example.usermgmt.userservice.dto.user.BatchUpdateUsersResponse;
import com.example.usermgmt.userservice.dto.user.ChangePasswordRequest;
import com.example.usermgmt.userservice.dto.user.CreateUserRequest;
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
import com.example.usermgmt.userservice.service.UserManagementService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.Data;
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
@RequestMapping("/internal/users")
public class UserController {

    private static final String USER_LIST = "menu:user:list";
    private static final String USER_CREATE = "menu:user:create";
    private static final String USER_UPDATE = "menu:user:update";
    private static final String USER_DELETE = "menu:user:delete";
    private static final String USER_RESET_PASSWORD = "menu:user:resetPassword";
    private static final String USER_ATTRIBUTE = "menu:user:attribute";
    private static final String USER_LOOKUP = "api:user:lookup";

    private final UserManagementService userManagementService;

    @PostMapping
    public ApiResponse<UserDetailResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        CurrentUserContext.requireMenuPermission(USER_CREATE);
        return ApiResponse.success("created", userManagementService.createUser(request, CurrentUserContext.requireUserId()));
    }

    @GetMapping
    public ApiResponse<PageResponse<UserListItemResponse>> listUsers(
        @RequestParam(value = "page", defaultValue = "1") Integer page,
        @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "role", required = false) String role,
        @RequestParam(value = "status", required = false) Integer status
    ) {
        CurrentUserContext.requireMenuPermission(USER_LIST);
        UserQueryRequest request = new UserQueryRequest();
        request.setPage(page);
        request.setPageSize(pageSize);
        request.setKeyword(keyword);
        request.setRole(role);
        request.setStatus(status);
        return ApiResponse.success(userManagementService.listUsers(request));
    }

    @GetMapping("/lookup")
    public ApiResponse<List<UserLookupResponse>> lookupUsers(@RequestParam(value = "keyword", required = false) String keyword) {
        CurrentUserContext.requireMenuPermission(USER_LOOKUP);
        return ApiResponse.success(userManagementService.lookupUsers(keyword));
    }

    @GetMapping("/{userId}/detail")
    public ApiResponse<UserDetailResponse> getUserDetail(@PathVariable("userId") String userId) {
        CurrentUserContext.requireMenuPermission(USER_LIST);
        return ApiResponse.success(userManagementService.getUserDetail(userId));
    }

    @GetMapping("/legacy/{legacyUserId}")
    public ApiResponse<String> getCanonicalUserId(@PathVariable Integer legacyUserId) {
        String userId = userManagementService.findUserIdByLegacyUserId(legacyUserId);
        if (userId == null) {
            return ApiResponse.fail(404, "legacy user not found");
        }
        return ApiResponse.success(userId);
    }

    @GetMapping("/{userId}/feishu-identity")
    public ApiResponse<FeishuIdentityResponse> getFeishuIdentity(@PathVariable("userId") String userId) {
        return ApiResponse.success(userManagementService.getFeishuIdentity(userId));
    }

    @GetMapping("/me")
    public ApiResponse<UserDetailResponse> getCurrentUser() {
        return ApiResponse.success(userManagementService.getCurrentUser(CurrentUserContext.requireUserId()));
    }

    @GetMapping("/me/menus")
    public ApiResponse<List<String>> getCurrentMenus() {
        return ApiResponse.success(userManagementService.getCurrentMenus(CurrentUserContext.requireUserId()));
    }

    @PutMapping
    public ApiResponse<BatchUpdateUsersResponse> updateUsers(@Valid @RequestBody UpdateUsersRequest request) {
        CurrentUserContext.requireMenuPermission(USER_UPDATE);
        return ApiResponse.success("updated", userManagementService.updateUsers(request, CurrentUserContext.requireUserId()));
    }

    @PutMapping("/me")
    public ApiResponse<UserDetailResponse> updateCurrentUser(@RequestBody UpdateCurrentUserRequest request) {
        return ApiResponse.success("updated", userManagementService.updateCurrentUser(CurrentUserContext.requireUserId(), request));
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<DeleteUserResponse> deleteUser(@PathVariable("userId") String userId, @RequestParam(value = "permanent", defaultValue = "false") boolean permanent) {
        CurrentUserContext.requireMenuPermission(USER_DELETE);
        DeleteUserResponse response = new DeleteUserResponse();
        response.setUserId(userId);
        response.setDeletedAt(userManagementService.deleteUser(userId, permanent, CurrentUserContext.requireUserId()));
        return ApiResponse.success("deleted", response);
    }

    @DeleteMapping("/batch")
    public ApiResponse<BatchOperationResponse> batchDeleteUsers(@RequestBody BatchDeleteRequest request) {
        CurrentUserContext.requireMenuPermission(USER_DELETE);
        return ApiResponse.success("deleted", userManagementService.batchDeleteUsers(request.getUserIds(), CurrentUserContext.requireUserId()));
    }

    @PostMapping("/me/cancel")
    public ApiResponse<Void> cancelCurrentUser() {
        userManagementService.deleteUser(CurrentUserContext.requireUserId(), false, CurrentUserContext.requireUserId());
        return ApiResponse.success("cancelled", null);
    }

    @PostMapping("/me/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userManagementService.changePassword(CurrentUserContext.requireUserId(), request);
        return ApiResponse.success("password changed", null);
    }

    @PostMapping("/{userId}/password/reset")
    public ApiResponse<ResetPasswordResponse> resetPassword(@PathVariable("userId") String userId, @RequestBody ResetPasswordRequest request) {
        CurrentUserContext.requireMenuPermission(USER_RESET_PASSWORD);
        String newPassword = userManagementService.resetPassword(userId, request, CurrentUserContext.requireUserId());
        ResetPasswordResponse response = new ResetPasswordResponse();
        response.setUserId(userId);
        response.setNewPassword(newPassword);
        return ApiResponse.success("password reset", response);
    }

    @PostMapping("/{userId}/attributes")
    public ApiResponse<UserAttributeResponse> createUserAttribute(@PathVariable("userId") String userId, @RequestBody UserAttributeRequest request) {
        requireSelfOrUserAttributePermission(userId);
        return ApiResponse.success("attribute created", userManagementService.createUserAttribute(userId, request));
    }

    @PutMapping("/{userId}/attributes/{attributeId}")
    public ApiResponse<UserAttributeResponse> updateUserAttribute(
        @PathVariable("userId") String userId,
        @PathVariable("attributeId") String attributeId,
        @RequestBody UserAttributeRequest request
    ) {
        requireSelfOrUserAttributePermission(userId);
        return ApiResponse.success("attribute updated", userManagementService.updateUserAttribute(userId, attributeId, request));
    }

    @DeleteMapping("/{userId}/attributes/{attributeId}")
    public ApiResponse<Void> deleteUserAttribute(@PathVariable("userId") String userId, @PathVariable("attributeId") String attributeId) {
        requireSelfOrUserAttributePermission(userId);
        userManagementService.deleteUserAttribute(userId, attributeId);
        return ApiResponse.success("attribute deleted", null);
    }

    @GetMapping("/{userId}/attributes")
    public ApiResponse<List<UserAttributeResponse>> listUserAttributes(@PathVariable("userId") String userId) {
        requireSelfOrUserAttributePermission(userId);
        return ApiResponse.success(userManagementService.listUserAttributes(userId));
    }

    @GetMapping("/me/attributes")
    public ApiResponse<List<UserAttributeResponse>> listCurrentUserAttributes() {
        return ApiResponse.success(userManagementService.listUserAttributes(CurrentUserContext.requireUserId()));
    }

    private void requireSelfOrUserAttributePermission(String userId) {
        String currentUserId = CurrentUserContext.requireUserId();
        if (!currentUserId.equals(userId)) {
            CurrentUserContext.requireMenuPermission(USER_ATTRIBUTE);
        }
    }

    @Data
    public static class ResetPasswordResponse {
        private String userId;
        private String newPassword;
    }

    @Data
    public static class DeleteUserResponse {
        private String userId;
        private java.time.LocalDateTime deletedAt;
    }

    @Data
    public static class BatchDeleteRequest {
        private List<String> userIds;
    }
}
