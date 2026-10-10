package com.example.usermgmt.userservice.dto.auth;

import java.util.List;
import lombok.Data;

@Data
public class UserAttributeExtraData {

    private String fieldKey;
    private String fieldType;
    private Boolean required;
    private Object value;
    private Object defaultValue;
    private List<String> options;
    private Integer sort;
}
