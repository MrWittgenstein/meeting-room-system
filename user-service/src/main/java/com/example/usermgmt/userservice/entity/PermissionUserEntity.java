package com.example.usermgmt.userservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("permission_and_user")
public class PermissionUserEntity {

    private String userId;
    private String permissionId;
    private String creatorId;
    private LocalDateTime createdTime;
    private LocalDateTime lastUpdateTime;
    private String description;
}
