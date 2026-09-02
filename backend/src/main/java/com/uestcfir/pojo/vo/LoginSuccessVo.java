package com.uestcfir.pojo.vo;


import lombok.Data;

@Data
public class LoginSuccessVo {
    String token;
    String userName;
    String userType;
    String avatarUrl;
}
