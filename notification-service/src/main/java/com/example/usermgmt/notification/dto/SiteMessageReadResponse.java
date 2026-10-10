package com.example.usermgmt.notification.dto;

import lombok.Data;

@Data
public class SiteMessageReadResponse {

    private Integer updatedCount;
    private Long unreadCount;
}
