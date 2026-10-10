package com.example.usermgmt.userservice.dto.user;

import lombok.Data;

@Data
public class UserLookupResponse {

    private String userId;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    private String feishuOpenId;
    private String feishuUserId;
    private String feishuUnionId;
}
