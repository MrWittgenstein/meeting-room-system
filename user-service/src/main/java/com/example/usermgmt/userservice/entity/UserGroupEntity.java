package com.example.usermgmt.userservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("user_and_group")
public class UserGroupEntity {

    private String userId;
    private String groupId;
    private String extension;
    private String creatorId;
    private LocalDateTime createdTime;
    private LocalDateTime lastUpdateTime;
    private String lastOperatorId;
    private String description;
    private String type;
}
