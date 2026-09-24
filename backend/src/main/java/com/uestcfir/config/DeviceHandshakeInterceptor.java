package com.uestcfir.config;

import com.uestcfir.mapper.IotDeviceMapper;
import com.uestcfir.mapper.MeetingroomMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import java.util.Map;

@Component
public class DeviceHandshakeInterceptor implements HandshakeInterceptor {
    private final DeviceAuthProperties properties;
    private final IotDeviceMapper devices;
    private final MeetingroomMapper rooms;

    public DeviceHandshakeInterceptor(DeviceAuthProperties properties, IotDeviceMapper devices, MeetingroomMapper rooms) {
        this.properties = properties;
        this.devices = devices;
        this.rooms = rooms;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler handler, Map<String, Object> attributes) {
        String deviceId = request.getHeaders().getFirst("X-Device-Id");
        if (!properties.valid(deviceId, request.getHeaders().getFirst("X-Device-Token"))) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        var device = devices.findByDeviceId(deviceId);
        if (device == null || device.getRoomId() == null || rooms.getMeetingroomById(device.getRoomId()) == null) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
        attributes.put("deviceId", deviceId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler handler, Exception exception) { }
}
