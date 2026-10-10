package com.example.usermgmt.userservice.dto.permission;

import lombok.Data;

@Data
public class PermissionQueryRequest {

    private String keyword;
    private Boolean tree = true;
}
