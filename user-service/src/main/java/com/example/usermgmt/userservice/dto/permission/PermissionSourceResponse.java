package com.example.usermgmt.userservice.dto.permission;

import java.util.List;
import lombok.Data;

@Data
public class PermissionSourceResponse {

    private String userId;
    private String permId;
    private String permCode;
    private String permName;
    private Boolean hasPermission;
    private List<PermissionSourceItem> sources;

    @Data
    public static class PermissionSourceItem {
        private String type;
        private String sourceId;
        private String sourceName;
        private String grantedAt;
    }
}
