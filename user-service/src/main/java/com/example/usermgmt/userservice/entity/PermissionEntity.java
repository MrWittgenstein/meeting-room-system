package com.example.usermgmt.userservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("permission")
public class PermissionEntity {

    @TableId(type = IdType.INPUT)
    private String id;
    private String code;
    private String name;
    private String parentId;
    private String path;
    private String icon;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;
    private String creatorId;
    private String description;
    private LocalDateTime lastUpdateTime;
    @TableField("method_type")
    private String methodType;
}
