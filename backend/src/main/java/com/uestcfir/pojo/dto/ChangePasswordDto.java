package com.uestcfir.pojo.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangePasswordDto {
    @NotNull(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱不能为空")
    private String email;

    @NotNull(message = "验证码不能为空")
    @NotBlank(message = "验证码不能为空")
    private String code;

    @NotNull(message = "新密码不能为空")
    @NotBlank(message = "新密码不能为空")
    private String newPassword;



}
