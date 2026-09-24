package com.uestcfir.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;

class AccessLogFilterTest {
    @Test
    void credentialsAreOmittedIdentityIsTrustedAndBusinessFailureRecorded() throws Exception {
        var mapper = new ObjectMapper();
        var logger = (Logger) LoggerFactory.getLogger("ACCESS_LOG");
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            var request = new MockHttpServletRequest("POST", "/user/login");
            request.setQueryString("password=secret-value&%74oken=secret-value");
            request.addHeader("Authorization", "Bearer secret-value");
            request.addHeader("X-User-Id", "forged");
            request.addHeader("X-Trace-Id", "test-trace");
            var response = new MockHttpServletResponse();
            new AccessLogFilter(mapper).doFilter(request, response, (req, res) -> {
                req.setAttribute("UserId", 18);
                req.setAttribute("businessCode", 0);
            });
            String text = appender.list.get(0).getFormattedMessage();
            var json = mapper.readTree(text);
            assertFalse(text.contains("secret-value"));
            assertFalse(text.contains("forged"));
            assertEquals("18", json.path("user_id").asText());
            assertEquals(0, json.path("business_code").asInt());
            assertEquals("test-trace", response.getHeader("X-Trace-Id"));
            assertNotNull(json.get("event_id"));
            assertNull(TraceContext.get());
        } finally {
            logger.detachAppender(appender);
        }
    }
}
