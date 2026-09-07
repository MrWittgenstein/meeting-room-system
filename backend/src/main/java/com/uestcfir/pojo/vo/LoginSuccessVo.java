package com.uestcfir.pojo.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginSuccessVo {
    String token;
    /** Explicit session field for new clients; token remains for old clients. */
    String sessionId;
    String userName;
    String userType;
    String avatarUrl;
    String role;
    Set<String> permissions;
}
