package com.example.usermgmt.userservice.dto.auth;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserExtraData {

    private String avatar;
    private String role;
    private String remark;
    private String feishuOpenId;
    private String feishuUnionId;
    private String feishuUserId;
    private String enterpriseEmail;
    private Integer failedPasswordAttempts;
    private LocalDateTime lockedUntil;
}
