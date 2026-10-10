package com.example.usermgmt.common.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthRegisterRequest {

    private String account;

    private String registerType;

    private String phone;
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String code;

    private String nickname;

    private String loginIp;

    public String getAccount() {
        if (account != null && !account.isBlank()) {
            return account.trim();
        }
        if (email != null && !email.isBlank()) {
            return email.trim();
        }
        if (phone != null && !phone.isBlank()) {
            return phone.trim();
        }
        return account;
    }
}
