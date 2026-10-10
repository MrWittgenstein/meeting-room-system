package com.example.usermgmt.userservice.dto.group;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class GroupAttributeResponse {

    private String attributeId;
    private String groupId;
    private String name;
    private String fieldKey;
    private String fieldType;
    private Boolean required;
    private Object value;
    private Object defaultValue;
    private List<String> options;
    private Integer sort;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
