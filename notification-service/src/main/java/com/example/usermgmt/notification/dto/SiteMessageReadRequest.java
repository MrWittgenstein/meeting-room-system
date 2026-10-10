package com.example.usermgmt.notification.dto;

import java.util.List;
import lombok.Data;

@Data
public class SiteMessageReadRequest {

    private List<String> messageIds;
    private Boolean markAll;
}
