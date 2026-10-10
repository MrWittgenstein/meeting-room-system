package com.example.usermgmt.userservice.controller;

import com.example.usermgmt.common.api.ApiResponse;
import com.example.usermgmt.common.api.PageResponse;
import com.example.usermgmt.userservice.context.CurrentUserContext;
import com.example.usermgmt.userservice.dto.common.BatchOperationRequest;
import com.example.usermgmt.userservice.dto.common.BatchOperationResponse;
import com.example.usermgmt.userservice.dto.group.GroupAttributeRequest;
import com.example.usermgmt.userservice.dto.group.GroupAttributeResponse;
import com.example.usermgmt.userservice.dto.group.GroupCreateRequest;
import com.example.usermgmt.userservice.dto.group.GroupJoinRequest;
import com.example.usermgmt.userservice.dto.group.GroupMemberOperateRequest;
import com.example.usermgmt.userservice.dto.group.GroupMemberUpgradeRequest;
import com.example.usermgmt.userservice.dto.group.GroupQueryRequest;
import com.example.usermgmt.userservice.dto.group.GroupResponse;
import com.example.usermgmt.userservice.dto.group.GroupUpdateRequest;
import com.example.usermgmt.userservice.service.GroupManagementService;
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
@RequestMapping("/internal/groups")
public class GroupController {

    private static final String GROUP_LIST = "menu:group:list";
    private static final String GROUP_CREATE = "menu:group:create";
    private static final String GROUP_UPDATE = "menu:group:update";
    private static final String GROUP_DELETE = "menu:group:delete";
    private static final String GROUP_MEMBER = "menu:group:member";
    private static final String GROUP_TEMP_PASSWORD = "menu:group:tempPassword";

    private final GroupManagementService groupManagementService;

    @PostMapping
    public ApiResponse<GroupResponse> createGroup(@Valid @RequestBody GroupCreateRequest request) {
        CurrentUserContext.requireMenuPermission(GROUP_CREATE);
        return ApiResponse.success("created", groupManagementService.createGroup(request, CurrentUserContext.requireUserId()));
    }

    @GetMapping
    public ApiResponse<PageResponse<GroupResponse>> listGroups(
        @RequestParam(value = "page", defaultValue = "1") Integer page,
        @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) Integer status
    ) {
        CurrentUserContext.requireMenuPermission(GROUP_LIST);
        GroupQueryRequest request = new GroupQueryRequest();
        request.setPage(page);
        request.setPageSize(pageSize);
        request.setKeyword(keyword);
        request.setStatus(status);
        return ApiResponse.success(groupManagementService.listGroups(request));
    }

    @GetMapping("/my")
    public ApiResponse<PageResponse<GroupResponse>> listMyGroups(
        @RequestParam(value = "page", defaultValue = "1") Integer page,
        @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) Integer status
    ) {
        GroupQueryRequest request = new GroupQueryRequest();
        request.setPage(page);
        request.setPageSize(pageSize);
        request.setKeyword(keyword);
        request.setStatus(status);
        return ApiResponse.success(groupManagementService.listMyGroups(CurrentUserContext.requireUserId(), request));
    }

    @GetMapping("/public")
    public ApiResponse<PageResponse<GroupResponse>> listPublicGroups(
        @RequestParam(value = "page", defaultValue = "1") Integer page,
        @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
        @RequestParam(value = "keyword", required = false) String keyword
    ) {
        GroupQueryRequest request = new GroupQueryRequest();
        request.setPage(page);
        request.setPageSize(pageSize);
        request.setKeyword(keyword);
        return ApiResponse.success(groupManagementService.listPublicGroups(request));
    }

    @GetMapping("/{groupId}")
    public ApiResponse<GroupResponse> getGroupDetail(@PathVariable("groupId") String groupId) {
        CurrentUserContext.requireMenuPermission(GROUP_LIST);
        return ApiResponse.success(groupManagementService.getGroupDetail(groupId));
    }

    @GetMapping("/{groupId}/my")
    public ApiResponse<GroupResponse> getMyGroupDetail(@PathVariable("groupId") String groupId) {
        return ApiResponse.success(groupManagementService.getMyGroupDetail(groupId, CurrentUserContext.requireUserId()));
    }

    @PutMapping
    public ApiResponse<BatchOperationResponse> updateGroups(@RequestBody GroupUpdateRequest request) {
        CurrentUserContext.requireMenuPermission(GROUP_UPDATE);
        return ApiResponse.success("updated", groupManagementService.updateGroups(request, CurrentUserContext.requireUserId()));
    }

    @DeleteMapping
    public ApiResponse<BatchOperationResponse> deleteGroups(@RequestBody BatchOperationRequest request) {
        CurrentUserContext.requireMenuPermission(GROUP_DELETE);
        return ApiResponse.success("deleted", groupManagementService.deleteGroups(request));
    }

    @PostMapping("/{groupId}/members")
    public ApiResponse<BatchOperationResponse> addMembers(@PathVariable("groupId") String groupId, @RequestBody GroupMemberOperateRequest request) {
        CurrentUserContext.requireMenuPermission(GROUP_MEMBER);
        return ApiResponse.success("members added", groupManagementService.addMembers(groupId, request, CurrentUserContext.requireUserId()));
    }

    @DeleteMapping("/{groupId}/members")
    public ApiResponse<BatchOperationResponse> removeMembers(@PathVariable("groupId") String groupId, @RequestBody GroupMemberOperateRequest request) {
        CurrentUserContext.requireMenuPermission(GROUP_MEMBER);
        return ApiResponse.success("members removed", groupManagementService.removeMembers(groupId, request));
    }

    @PutMapping("/{groupId}/members/{userId}/upgrade")
    public ApiResponse<GroupResponse> upgradeMember(@PathVariable("groupId") String groupId, @PathVariable("userId") String userId, @RequestBody GroupMemberUpgradeRequest request) {
        CurrentUserContext.requireMenuPermission(GROUP_MEMBER);
        return ApiResponse.success("member updated", groupManagementService.upgradeMember(groupId, userId, request, CurrentUserContext.requireUserId()));
    }

    @PostMapping("/{groupId}/temp-password")
    public ApiResponse<GroupResponse> setTempPassword(@PathVariable("groupId") String groupId, @RequestBody TempPasswordPayload request) {
        CurrentUserContext.requireMenuPermission(GROUP_TEMP_PASSWORD);
        return ApiResponse.success("temp password set", groupManagementService.setTempPassword(groupId, request.getTempPassword(), request.getExpireHours()));
    }

    @DeleteMapping("/{groupId}/temp-password")
    public ApiResponse<Void> clearTempPassword(@PathVariable("groupId") String groupId) {
        CurrentUserContext.requireMenuPermission(GROUP_TEMP_PASSWORD);
        groupManagementService.clearTempPassword(groupId);
        return ApiResponse.success("temp password cleared", null);
    }

    @PostMapping("/{groupId}/join")
    public ApiResponse<GroupResponse> joinGroup(@PathVariable("groupId") String groupId, @RequestBody GroupJoinRequest request) {
        return ApiResponse.success("joined", groupManagementService.joinGroup(groupId, CurrentUserContext.requireUserId(), request));
    }

    @PostMapping("/{groupId}/attributes")
    public ApiResponse<GroupAttributeResponse> createAttribute(@PathVariable("groupId") String groupId, @RequestBody GroupAttributeRequest request) {
        CurrentUserContext.requireMenuPermission(GROUP_UPDATE);
        return ApiResponse.success("attribute created", groupManagementService.createGroupAttribute(groupId, request));
    }

    @PutMapping("/{groupId}/attributes/{attributeId}")
    public ApiResponse<GroupAttributeResponse> updateAttribute(@PathVariable("groupId") String groupId, @PathVariable("attributeId") String attributeId, @RequestBody GroupAttributeRequest request) {
        CurrentUserContext.requireMenuPermission(GROUP_UPDATE);
        return ApiResponse.success("attribute updated", groupManagementService.updateGroupAttribute(groupId, attributeId, request));
    }

    @DeleteMapping("/{groupId}/attributes/{attributeId}")
    public ApiResponse<Void> deleteAttribute(@PathVariable("groupId") String groupId, @PathVariable("attributeId") String attributeId) {
        CurrentUserContext.requireMenuPermission(GROUP_UPDATE);
        groupManagementService.deleteGroupAttribute(groupId, attributeId);
        return ApiResponse.success("attribute deleted", null);
    }

    @GetMapping("/{groupId}/attributes")
    public ApiResponse<List<GroupAttributeResponse>> listAttributes(@PathVariable("groupId") String groupId) {
        CurrentUserContext.requireMenuPermission(GROUP_LIST);
        return ApiResponse.success(groupManagementService.listGroupAttributes(groupId));
    }

    @Data
    public static class TempPasswordPayload {
        private String tempPassword;
        private Integer expireHours;
    }
}
