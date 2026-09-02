package com.uestcfir.config;

import com.uestcfir.websocket.IotDeviceWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class IotDeviceWebSocketConfig implements WebSocketConfigurer {
    @Autowired
    private IotDeviceWebSocketHandler iotDeviceWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(iotDeviceWebSocketHandler, "/iot/ws/device")
                .setAllowedOriginPatterns("*");
    }
}
