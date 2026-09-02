package com.uestcfir.pojo.dto;


import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserDto {
    @NotNull(message = "用户名不能为空")
    @NotBlank(message = "用户名不能为空")
    private String username;
    private String realName;
    @NotNull(message = "验证码不能为空")
    @NotBlank(message = "验证码不能为空")
    private String code;
    @Size(min = 6, max = 16, message = "密码长度必须在6-16位之间")
    @Pattern(regexp = "^(?![0-9]+$)(?![a-zA-Z]+$)[0-9A-Za-z]{6,16}$", message = "密码必须包含字母和数字，长度在6-16位之间")
    @NotNull(message = "密码不能为空")
    @NotBlank(message = "密码不能为空")
    private String password;
    @Email(message = "邮箱格式不正确")
    @NotNull(message = "邮箱不能为空")
    @NotBlank(message = "邮箱不能为空")
    private String email;
    private String phone;
    private Integer userType;

}
