package com.example.usermgmt.userservice.dto.user;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class UserAttributeResponse {

    private String attributeId;
    private String userId;
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
