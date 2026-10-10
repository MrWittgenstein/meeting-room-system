package com.example.usermgmt.userservice.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateUserRequest {

    private String phone;
    private String email;

    @NotBlank
    private String password;

    private String nickname;
    private String avatar;
    private String role;
    private Integer status;
    private String remark;
}
