package com.example.usermgmt.userservice.dto.permission;

import com.example.usermgmt.userservice.dto.group.PermissionNodeResponse;
import java.util.List;
import lombok.Data;

@Data
public class UserEffectivePermissionsResponse {

    private String userId;
    private String nickname;
    private List<UserGroupSummary> groups;
    private List<PermissionNodeResponse> permissions;

    @Data
    public static class UserGroupSummary {
        private String groupId;
        private String groupName;
    }
}
