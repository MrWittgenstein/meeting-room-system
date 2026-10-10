package com.example.usermgmt.userservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("permission_and_group")
public class PermissionGroupEntity {

    private String groupId;
    private String permissionId;
    private LocalDateTime createdTime;
    private LocalDateTime lastUpdateTime;
    private String creatorId;
    private String description;
}
