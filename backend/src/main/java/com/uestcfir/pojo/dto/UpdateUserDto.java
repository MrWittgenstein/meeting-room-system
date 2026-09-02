package com.uestcfir.pojo.dto;

import lombok.Data;

@Data
public class UpdateUserDto {
    private String username;
    private String realName;
    private String phone;
}