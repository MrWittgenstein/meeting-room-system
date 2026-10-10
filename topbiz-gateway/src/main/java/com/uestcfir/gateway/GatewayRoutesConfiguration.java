package com.uestcfir.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class GatewayRoutesConfiguration {
    @Bean
    public CorsWebFilter corsWebFilter(
            @Value("${CORS_ALLOWED_ORIGINS:http://localhost:*,http://127.0.0.1:*,http://8.141.97.91:*}") String allowedOrigins) {
        CorsConfiguration cors = new CorsConfiguration();
        Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .forEach(cors::addAllowedOriginPattern);
        cors.setAllowCredentials(true);
        cors.addAllowedHeader(CorsConfiguration.ALL);
        cors.addAllowedMethod(CorsConfiguration.ALL);
        cors.addExposedHeader(HttpHeaders.SET_COOKIE);
        cors.addExposedHeader("X-Trace-Id");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return new CorsWebFilter(source);
    }

    @Bean
    public RouteLocator meetingroomRoute(
            RouteLocatorBuilder builder,
            @Value("${MEETINGROOM_SERVICE_URL:http://127.0.0.1:8084}") String meetingroomUrl,
            @Value("${MEETINGROOM_WS_URL:ws://127.0.0.1:8084}") String meetingroomWsUrl,
            @Value("${GATEWAY_DISCOVERY_ENABLED:false}") boolean discoveryEnabled,
            @Value("${LOG_SERVICE_URL:http://127.0.0.1:8083}") String logUrl) {
        String meetingroomHttpTarget = discoveryEnabled ? "lb://meetingroom-main" : meetingroomUrl;
        String meetingroomWebSocketTarget = discoveryEnabled ? "lb:ws://meetingroom-main" : meetingroomWsUrl;
        return builder.routes()
                .route("log-service", route -> route.path("/api/v1/logs/**").uri(logUrl))
                .route("meetingroom-device-websocket", route -> route.path("/iot/ws/device").uri(meetingroomWebSocketTarget))
                .route("meetingroom-service", route -> route.path("/**").uri(meetingroomHttpTarget))
                .build();
    }
}
