package com.example.usermgmt.userservice.dto.group;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class PermissionNodeResponse {

    private String permId;
    private String permCode;
    private String permName;
    private String parentId;
    private String path;
    private String icon;
    private Integer sort;
    private String description;
    private String grantType;
    private String source;
    private String grantedAt;
    private List<PermissionNodeResponse> children = new ArrayList<>();
}
