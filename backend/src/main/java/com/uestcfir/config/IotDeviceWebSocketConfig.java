package com.uestcfir.config;

import com.uestcfir.websocket.IotDeviceWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@org.springframework.boot.context.properties.EnableConfigurationProperties(DeviceAuthProperties.class)
public class IotDeviceWebSocketConfig implements WebSocketConfigurer {
    @Autowired
    private IotDeviceWebSocketHandler iotDeviceWebSocketHandler;
    @Autowired
    private DeviceHandshakeInterceptor deviceHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(iotDeviceWebSocketHandler, "/iot/ws/device")
                .addInterceptors(deviceHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
