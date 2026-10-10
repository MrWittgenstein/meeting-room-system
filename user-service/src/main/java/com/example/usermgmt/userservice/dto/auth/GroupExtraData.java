package com.example.usermgmt.userservice.dto.auth;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class GroupExtraData {

    private String tempPassword;
    private LocalDateTime tempPasswordExpireAt;
}
