package com.uestcfir.service;

import com.uestcfir.auth.CurrentUserContext;
import com.uestcfir.auth.SessionUser;
import com.uestcfir.client.NotificationServiceClient;
import com.uestcfir.controller.admin.ImportantNewsController;
import com.uestcfir.mapper.ImportantNewsMapper;
import com.uestcfir.pojo.entity.ImportantNews;
import com.uestcfir.service.impl.ImportantNewsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AnnouncementEventTest {
    @AfterEach void clear() { CurrentUserContext.clear(); }
    @Test void normalPublishedStatusEmitsEventAndUsesTrustedPublisher() {
        CurrentUserContext.set(SessionUser.builder().userId(12).username("admin").permissions(Set.of("news:manage")).build());
        ImportantNewsService news = mock(ImportantNewsService.class);
        NotificationServiceClient notifications = mock(NotificationServiceClient.class);
        ImportantNewsController controller = new ImportantNewsController(news, null, notifications);
        ImportantNews item = new ImportantNews(); item.setStatus("未过期");
        when(news.addImportantNews(item)).thenReturn(true);
        assertTrue(controller.addImportantNews(item));
        assertEquals(12, item.getPublisherId()); assertEquals("admin", item.getPublisherName());
        verify(notifications).publishAnnouncement(item, 12);
    }
    @Test void legacyPublishedStatusAndDefaultMapToDatabaseEnum() {
        ImportantNewsMapper mapper = mock(ImportantNewsMapper.class);
        ImportantNewsServiceImpl service = new ImportantNewsServiceImpl();
        ReflectionTestUtils.setField(service, "importantNewsMapper", mapper);
        for (String status : new String[] { null, "已发布" }) {
            ImportantNews item = new ImportantNews(); item.setTitle("test"); item.setContent("body"); item.setStatus(status);
            service.addImportantNews(item); assertEquals("未过期", item.getStatus());
        }
    }
    @Test void eventEnqueueFailurePropagatesFromAdapter() {
        var properties = new com.uestcfir.config.PlatformServiceProperties(); properties.setEnabled(true);
        var users = mock(com.uestcfir.client.UserServiceClient.class);
        var publisher = mock(com.uestcfir.messaging.MeetingEventPublisher.class);
        var client = new NotificationServiceClient(properties, users, publisher);
        ImportantNews item = new ImportantNews();
        when(users.listUserIds()).thenReturn(java.util.List.of("12"));
        when(publisher.publishAnnouncement(item, 12, java.util.List.of("12"))).thenThrow(new IllegalStateException("db error"));
        assertThrows(IllegalStateException.class, () -> client.publishAnnouncement(item, 12));
    }
}
