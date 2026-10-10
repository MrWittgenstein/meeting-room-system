package com.example.usermgmt.userservice.dto.user;

import lombok.Data;

@Data
public class UserQueryRequest {

    private Integer page = 1;
    private Integer pageSize = 20;
    private String keyword;
    private String role;
    private Integer status;
}
