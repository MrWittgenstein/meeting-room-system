package com.example.usermgmt.userservice.dto.user;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class UserListItemResponse {

    private String userId;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    private String role;
    private List<String> groupNames;
    private String status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
