package com.example.usermgmt.userservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName(value = "`group`")
public class GroupEntity {

    @TableId(type = IdType.INPUT)
    private String id;
    private String name;
    private String parentId;
    private String type;
    private Integer status;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime lastUpdateTime;
    private String creatorId;
    private String extension;
    @TableField("extra_data")
    private String extraData;
}
