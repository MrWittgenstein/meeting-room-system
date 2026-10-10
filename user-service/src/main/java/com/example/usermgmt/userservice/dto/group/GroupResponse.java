package com.example.usermgmt.userservice.dto.group;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class GroupResponse {

    private String groupId;
    private String groupName;
    private String description;
    private String status;
    private String myRole;
    private Integer memberCount;
    private List<String> memberNames;
    private Boolean hasTempPassword;
    private LocalDateTime tempPasswordExpireAt;
    private LocalDateTime tempExpireAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<GroupMemberResponse> members;
    private List<PermissionNodeResponse> permissions;
    private List<GroupAttributeResponse> attributes;
}
