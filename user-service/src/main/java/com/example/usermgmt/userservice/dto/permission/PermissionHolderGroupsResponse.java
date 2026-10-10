package com.example.usermgmt.userservice.dto.permission;

import java.util.List;
import lombok.Data;

@Data
public class PermissionHolderGroupsResponse {

    private PermissionResponse permission;
    private Long total;
    private Integer page;
    private Integer pageSize;
    private List<GroupItem> list;

    @Data
    public static class GroupItem {
        private String groupId;
        private String groupName;
        private String description;
        private String status;
        private Integer memberCount;
        private String grantedAt;
        private String expireAt;
        private Boolean isActive;
    }
}
