package com.example.usermgmt.notification.service;

import com.example.usermgmt.notification.dto.SiteMessageListResponse;
import com.example.usermgmt.notification.dto.SiteMessageReadRequest;
import com.example.usermgmt.notification.dto.SiteMessageReadResponse;
import com.example.usermgmt.notification.dto.SiteMessageSendRequest;

public interface SiteMessageService {

    Integer send(SiteMessageSendRequest request);

    SiteMessageListResponse list(String userId, Integer page, Integer pageSize, Boolean readStatus);

    SiteMessageReadResponse markRead(String userId, SiteMessageReadRequest request);
}
