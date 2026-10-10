package com.example.usermgmt.userservice.service;

import com.example.usermgmt.common.api.PageResponse;
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

public interface GroupManagementService {

    GroupResponse createGroup(GroupCreateRequest request, String operatorId);

    PageResponse<GroupResponse> listGroups(GroupQueryRequest request);

    GroupResponse getGroupDetail(String groupId);

    BatchOperationResponse updateGroups(GroupUpdateRequest request, String operatorId);

    BatchOperationResponse addMembers(String groupId, GroupMemberOperateRequest request, String operatorId);

    BatchOperationResponse removeMembers(String groupId, GroupMemberOperateRequest request);

    PageResponse<GroupResponse> listMyGroups(String userId, GroupQueryRequest request);

    PageResponse<GroupResponse> listPublicGroups(GroupQueryRequest request);

    GroupResponse getMyGroupDetail(String groupId, String userId);

    GroupResponse setTempPassword(String groupId, String tempPassword, Integer expireHours);

    void clearTempPassword(String groupId);

    GroupResponse joinGroup(String groupId, String userId, GroupJoinRequest request);

    GroupResponse upgradeMember(String groupId, String userId, GroupMemberUpgradeRequest request, String operatorId);

    BatchOperationResponse deleteGroups(BatchOperationRequest request);

    GroupAttributeResponse createGroupAttribute(String groupId, GroupAttributeRequest request);

    GroupAttributeResponse updateGroupAttribute(String groupId, String attributeId, GroupAttributeRequest request);

    void deleteGroupAttribute(String groupId, String attributeId);

    java.util.List<GroupAttributeResponse> listGroupAttributes(String groupId);
}
