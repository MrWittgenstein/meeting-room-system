package com.example.usermgmt.userservice.dto.group;

import java.util.List;
import lombok.Data;

@Data
public class GroupUpdateRequest {

    private List<String> groupIds;
    private String groupName;
    private String description;
    private Integer status;
}
