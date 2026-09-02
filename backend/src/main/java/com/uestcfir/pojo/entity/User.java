package com.uestcfir.pojo.entity;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * @author ylshen
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private Integer userId;
    private String username;
    private String realName;
    private String password;
    private String email;
    private String phone;
    private Integer userType;//用户类型 0：会议室管理员 1：普通用户 2：超级管理员
    private Integer status;//状态 0：正常 1：冻结
    private Integer integral;//积分
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String emailHash;
    private String originUrl;
    private String thumbnailUrl;

    public User(Integer userId, String username, String realName, String password,
                String email, String emailHash, String phone, Integer userType,
                Integer status, Integer integral, LocalDateTime createTime, LocalDateTime updateTime) {
        this.userId = userId;
        this.username = username;
        this.realName = realName;
        this.password = password;
        this.email = email;
        this.emailHash = emailHash;
        this.phone = phone;
        this.userType = userType;
        this.status = status;
        this.integral = integral;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }


}
