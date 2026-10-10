package com.example.usermgmt.userservice.dto.user;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class UserDetailResponse {

    private String userId;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    private String role;
    private List<String> groupIds;
    private List<String> groupNames;
    private String status;
    private String remark;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<UserAttributeResponse> attributes;
}
