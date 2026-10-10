package com.example.usermgmt.notification.dto;

import java.util.List;
import lombok.Data;

@Data
public class SiteMessageListResponse {

    private Long total;
    private Integer page;
    private Integer pageSize;
    private Long unreadCount;
    private List<SiteMessageItemResponse> list;
}
