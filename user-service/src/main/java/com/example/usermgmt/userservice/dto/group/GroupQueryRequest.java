package com.example.usermgmt.userservice.dto.group;

import lombok.Data;

@Data
public class GroupQueryRequest {

    private Integer page = 1;
    private Integer pageSize = 20;
    private String keyword;
    private Integer status;
}
