package com.example.usermgmt.userservice.dto.permission;

import java.util.List;
import lombok.Data;

@Data
public class PermissionHolderUsersResponse {

    private PermissionResponse permission;
    private Long total;
    private Integer page;
    private Integer pageSize;
    private List<UserItem> list;

    @Data
    public static class UserItem {
        private String userId;
        private String nickname;
        private String avatar;
        private String email;
        private String sourceType;
        private SourceDetail sourceDetail;
        private String grantedBy;
        private String grantedByName;
        private String grantedAt;
        private String expireAt;
        private Boolean isActive;
    }

    @Data
    public static class SourceDetail {
        private String groupId;
        private String groupName;
        private String role;
        private String joinedAt;
    }
}
