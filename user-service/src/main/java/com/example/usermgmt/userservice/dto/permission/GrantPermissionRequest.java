package com.example.usermgmt.userservice.dto.permission;

import java.util.List;
import lombok.Data;

@Data
public class GrantPermissionRequest {

    private List<String> permIds;
    private Boolean overwrite;
}
