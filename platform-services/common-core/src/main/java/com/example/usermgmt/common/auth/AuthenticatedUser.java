package com.example.usermgmt.common.auth;

import java.util.List;
import lombok.Data;

@Data
public class AuthenticatedUser {

    private String userId;
    private String nickname;
    private String avatar;
    private Integer status;
    private String role;
    private List<String> menus;
}
