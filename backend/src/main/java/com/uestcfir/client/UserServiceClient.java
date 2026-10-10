package com.uestcfir.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uestcfir.auth.CurrentUserContext;
import com.uestcfir.config.PlatformServiceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.List;

/** Read-only user adapter used during the migration. */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {
    private final RestTemplate platformRestTemplate;
    private final PlatformServiceProperties properties;
    private final ObjectMapper objectMapper;

    public JsonNode getCurrentUser() {
        if (!properties.isEnabled()) {
            return null;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
            String token = properties.getInternalToken();
            if (token != null && !token.isBlank()) {
                headers.setBearerAuth(token);
            }
            var current = CurrentUserContext.requireUser();
            headers.set("X-User-Id", String.valueOf(current.getUserId()));
            headers.set("X-User-Role", current.getRole());
            headers.set("X-User-Menus", String.join(",", current.getPermissions()));
            String body = platformRestTemplate.exchange(
                    userServiceBaseUrl() + "/internal/users/me",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class).getBody();
            JsonNode root = objectMapper.readTree(body);
            return root.path("data");
        } catch (Exception ex) {
            log.warn("user-service lookup failed: {}", ex.getMessage());
            return null;
        }
    }

    public List<String> listUserIds() {
        if (!properties.isEnabled()) {
            return List.of();
        }
        try {
            var current = CurrentUserContext.requireUser();
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("X-User-Id", String.valueOf(current.getUserId()));
            headers.set("X-User-Role", current.getRole());
            java.util.Set<String> menus = new java.util.LinkedHashSet<>(current.getPermissions());
            menus.add("menu:user:list");
            headers.set("X-User-Menus", String.join(",", menus));
            String url = userServiceBaseUrl() + "/internal/users?page=%d&pageSize=100&status=1";
            List<String> userIds = new ArrayList<>();
            for (int page = 1; page <= 100; page++) {
                String body = platformRestTemplate.exchange(String.format(url, page), HttpMethod.GET,
                        new HttpEntity<>(headers), String.class).getBody();
                JsonNode root = objectMapper.readTree(body);
                JsonNode data = root.path("data");
                JsonNode records = data.path("list");
                if (!records.isArray() || records.isEmpty()) {
                    break;
                }
                records.forEach(record -> {
                    String id = record.path("userId").asText();
                    if (!id.isBlank()) userIds.add(id);
                });
                long total = data.path("total").asLong(userIds.size());
                if ((long) page * 100 >= total) break;
            }
            return userIds;
        } catch (Exception ex) {
            log.warn("user-service recipient lookup failed: {}", ex.getMessage());
            return List.of();
        }
    }

    public String resolveCanonicalUserId(Integer legacyUserId) {
        if (!properties.isEnabled() || legacyUserId == null) {
            return legacyUserId == null ? null : String.valueOf(legacyUserId);
        }
        try {
            String body = platformRestTemplate.getForObject(
                    userServiceBaseUrl() + "/internal/users/legacy/" + legacyUserId,
                    String.class);
            JsonNode root = objectMapper.readTree(body);
            String userId = root.path("data").asText();
            return userId == null || userId.isBlank() ? String.valueOf(legacyUserId) : userId;
        } catch (Exception ex) {
            log.warn("canonical user lookup failed for legacy user {}: {}", legacyUserId, ex.getMessage());
            return String.valueOf(legacyUserId);
        }
    }

    private String userServiceBaseUrl() {
        return properties.isDiscoveryEnabled()
                ? "http://" + properties.getUserServiceName()
                : properties.getUserServiceUrl();
    }
}
