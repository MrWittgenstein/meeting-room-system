package com.example.usermgmt.userservice.dto.user;

import lombok.Data;

@Data
public class FeishuIdentityResponse {

    private String userId;
    private String feishuOpenId;
    private String feishuUserId;
    private String feishuUnionId;
    private String enterpriseEmail;
}
