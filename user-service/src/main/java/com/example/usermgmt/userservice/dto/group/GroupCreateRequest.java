package com.example.usermgmt.userservice.dto.group;

import java.util.List;
import lombok.Data;

@Data
public class GroupCreateRequest {

    private String groupName;
    private String description;
    private List<String> initialMembers;
    private String tempPassword;
    private Integer tempPasswordExpireHours;
}
