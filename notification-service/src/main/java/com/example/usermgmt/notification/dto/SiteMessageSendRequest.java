package com.example.usermgmt.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.Data;

@Data
public class SiteMessageSendRequest {

    @NotEmpty(message = "接收人不能为空")
    private List<String> userIds;

    @NotBlank(message = "消息标题不能为空")
    private String title;

    @NotBlank(message = "消息内容不能为空")
    private String content;

    private String category;
    private String link;
    private String creatorId;
    private String templateCode;
    private JsonNode params;
}
