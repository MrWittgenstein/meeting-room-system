package com.example.usermgmt.userservice.dto.permission;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PermissionResponse {

    private String permId;
    private String permCode;
    private String permName;
    private String parentId;
    private String path;
    private String icon;
    private Integer sort;
    private String description;
    private String methodType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
