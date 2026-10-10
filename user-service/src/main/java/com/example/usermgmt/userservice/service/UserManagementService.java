package com.example.usermgmt.userservice.service;

import com.example.usermgmt.common.api.PageResponse;
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
import java.util.List;

public interface UserManagementService {

    UserDetailResponse createUser(CreateUserRequest request, String operatorId);

    PageResponse<UserListItemResponse> listUsers(UserQueryRequest request);

    List<UserLookupResponse> lookupUsers(String keyword);

    UserDetailResponse getUserDetail(String userId);

    String findUserIdByLegacyUserId(Integer legacyUserId);

    FeishuIdentityResponse getFeishuIdentity(String userId);

    UserDetailResponse getCurrentUser(String currentUserId);

    BatchUpdateUsersResponse updateUsers(UpdateUsersRequest request, String operatorId);

    UserDetailResponse updateCurrentUser(String currentUserId, UpdateCurrentUserRequest request);

    void changePassword(String currentUserId, ChangePasswordRequest request);

    String resetPassword(String targetUserId, ResetPasswordRequest request, String operatorId);

    List<String> getCurrentMenus(String currentUserId);

    java.time.LocalDateTime deleteUser(String userId, boolean permanent, String operatorId);

    BatchOperationResponse batchDeleteUsers(java.util.List<String> userIds, String operatorId);

    UserAttributeResponse createUserAttribute(String userId, UserAttributeRequest request);

    UserAttributeResponse updateUserAttribute(String userId, String attributeId, UserAttributeRequest request);

    void deleteUserAttribute(String userId, String attributeId);

    List<UserAttributeResponse> listUserAttributes(String userId);
}
