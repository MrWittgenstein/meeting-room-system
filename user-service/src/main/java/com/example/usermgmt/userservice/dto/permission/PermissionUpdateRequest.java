package com.example.usermgmt.userservice.dto.permission;

import lombok.Data;

@Data
public class PermissionUpdateRequest {

    private String permName;
    private String parentId;
    private String path;
    private String icon;
    private Integer sort;
    private String description;
}
