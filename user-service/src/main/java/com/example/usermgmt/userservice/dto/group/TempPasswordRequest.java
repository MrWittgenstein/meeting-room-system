package com.example.usermgmt.userservice.dto.group;

import lombok.Data;

@Data
public class TempPasswordRequest {

    private String tempPassword;
    private Integer expireHours;
}
