package com.example.usermgmt.userservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("attribute_data")
public class AttributeDataEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    private String name;
    private String description;

    @TableField("extra_data")
    private String extraData;

    @TableField("group_id")
    private String groupId;

    @TableField("user_id")
    private String userId;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("last_update_time")
    private LocalDateTime lastUpdateTime;
}
