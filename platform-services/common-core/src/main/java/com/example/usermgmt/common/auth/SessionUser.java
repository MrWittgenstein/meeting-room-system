package com.example.usermgmt.common.auth;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class SessionUser implements Serializable {

    private String sessionId;
    private String userId;
    private String nickname;
    private String avatar;
    private Integer status;
    private String role;
    private List<String> menus;

    private String deviceId;
    private String deviceType;
    private String deviceName;
    private String os;
    private String browser;
    private String loginIp;
    private LocalDateTime loginAt;
    private LocalDateTime lastActiveAt;
    private Boolean current;
}
