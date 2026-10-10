package com.example.usermgmt.userservice.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FailItemResponse {

    private String userId;
    private String reason;
}
