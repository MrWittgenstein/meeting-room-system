package com.uestcfir.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 原生 WebSocket 端点
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");

        // SockJS 回退端点
        registry.addEndpoint("/ws-sockjs")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 启用一个简单的基于内存的消息代理，用于向客户端发送消息
        registry.enableSimpleBroker("/topic", "/queue");

        // 设置应用程序目的地前缀，客户端发送消息时需要此前缀
        registry.setApplicationDestinationPrefixes("/app");

        // 设置用户目的地前缀，用于点对点消息传递
        registry.setUserDestinationPrefix("/user");
    }
}