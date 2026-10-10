package com.example.usermgmt.notification.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SiteMessageItemResponse {

    private String messageId;
    private String userId;
    private String title;
    private String content;
    private String category;
    private String link;
    private Boolean read;
    private String creatorId;
    private String creatorName;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
