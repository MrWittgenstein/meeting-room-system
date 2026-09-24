package com.uestcfir.config;

import com.uestcfir.mapper.IotDeviceMapper;
import com.uestcfir.mapper.MeetingroomMapper;
import com.uestcfir.pojo.entity.IotDevice;
import com.uestcfir.pojo.entity.Meetingroom;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeviceHandshakeTest {
    @Test
    void credentialsAreBoundToRegisteredDeviceAndValidRoom() {
        var properties = new DeviceAuthProperties();
        String secret = "test-device-credential-32-characters";
        properties.setTokens(Map.of("raspi-01", secret));
        var devices = mock(IotDeviceMapper.class);
        var rooms = mock(MeetingroomMapper.class);
        var interceptor = new DeviceHandshakeInterceptor(properties, devices, rooms);
        var request = new MockHttpServletRequest();
        request.addHeader("X-Device-Id", "raspi-01");
        for (int i = 0; i < 5; i++) {
            request.removeHeader("X-Device-Token");
            request.addHeader("X-Device-Token", "invalid-" + i);
            assertFalse(interceptor.beforeHandshake(new ServletServerHttpRequest(request),
                    new ServletServerHttpResponse(new MockHttpServletResponse()), null, new HashMap<>()));
        }
        verifyNoInteractions(devices);
        request.removeHeader("X-Device-Token");
        request.addHeader("X-Device-Token", secret);
        assertFalse(interceptor.beforeHandshake(new ServletServerHttpRequest(request),
                new ServletServerHttpResponse(new MockHttpServletResponse()), null, new HashMap<>()));
        var device = new IotDevice();
        device.setRoomId(14);
        when(devices.findByDeviceId("raspi-01")).thenReturn(device);
        when(rooms.getMeetingroomById(14)).thenReturn(new Meetingroom());
        var attributes = new HashMap<String, Object>();
        assertTrue(interceptor.beforeHandshake(new ServletServerHttpRequest(request),
                new ServletServerHttpResponse(new MockHttpServletResponse()), null, attributes));
        assertEquals("raspi-01", attributes.get("deviceId"));
    }
}
