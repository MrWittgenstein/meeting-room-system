package com.example.usermgmt.userservice.dto.group;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class GroupMemberResponse {

    private String userId;
    private String nickname;
    private String avatar;
    private String role;
    private LocalDateTime joinedAt;
}
