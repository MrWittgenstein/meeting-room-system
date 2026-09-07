package com.uestcfir.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The authenticated principal stored in Redis and exposed to the request.
 * The legacy userType field is retained for response compatibility only.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionUser {
    private String sessionId;
    private Integer userId;
    private String username;
    private Integer userType;
    private String role;
    @Builder.Default
    private Set<String> permissions = new LinkedHashSet<>();
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessAt;
}
