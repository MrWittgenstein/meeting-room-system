package com.example.usermgmt.userservice.dto.user;

import lombok.Data;

@Data
public class UpdateCurrentUserRequest {

    private String nickname;
    private String avatar;
}
