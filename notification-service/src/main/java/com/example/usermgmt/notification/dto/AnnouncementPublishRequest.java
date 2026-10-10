package com.example.usermgmt.notification.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

/** Compatibility command used while meeting-room announcements move to notification-service. */
@Data
public class AnnouncementPublishRequest {
    @NotEmpty
    private List<String> userIds;
    @NotBlank
    private String title;
    @NotBlank
    private String content;
    private String category;
    private String link;
    private String creatorId;
    private JsonNode params;
}
