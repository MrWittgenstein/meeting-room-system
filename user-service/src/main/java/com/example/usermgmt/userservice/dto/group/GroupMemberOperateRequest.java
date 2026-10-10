package com.example.usermgmt.userservice.dto.group;

import java.util.List;
import lombok.Data;

@Data
public class GroupMemberOperateRequest {

    private List<String> userIds;
    private String role;
}
