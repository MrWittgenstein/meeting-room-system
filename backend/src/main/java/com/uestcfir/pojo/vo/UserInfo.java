package com.uestcfir.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfo {
    private Integer userId;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private Integer integral;//积分
    private String thumbnailUrl;//头像URL

}
