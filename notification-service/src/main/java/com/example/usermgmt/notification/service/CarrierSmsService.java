package com.example.usermgmt.notification.service;

import com.example.usermgmt.common.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CarrierSmsService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public void sendCaptchaWithDefaultCarrier(String phone, String scene, String code) {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("code", code);
        sendTextSms(
            loadDefaultSmsConfig(),
            phone,
            null,
            null,
            params
        );
    }

    public void sendTextSms(JsonNode config, String phone, String title, String content, JsonNode params) {
        ensureConfigObject(config);
        String mode = defaultString(textValue(config, "mode", "type"), "template").trim().toLowerCase(Locale.ROOT);
        switch (mode) {
            case "template", "send" -> sendTemplateSms(config, phone, title, content, params);
            case "xsend", "webhook" -> sendXsendSms(config, phone, title, content, params);
            default -> throw new BusinessException(400, "Unsupported sms carrier mode: " + mode);
        }
    }

    private JsonNode loadDefaultSmsConfig() {
        List<JsonNode> configs = jdbcTemplate.query(
            """
            SELECT config_json
            FROM `carrier`
            WHERE channel_type = 'sms' AND status = 'active'
            ORDER BY is_default DESC, priority ASC, update_time DESC
            LIMIT 1
            """,
            (rs, rowNum) -> parseConfig(rs.getString("config_json"))
        );
        if (configs.isEmpty()) {
            throw new BusinessException(400, "No active sms carrier configured");
        }
        return configs.get(0);
    }

    private void sendTemplateSms(JsonNode config, String phone, String title, String content, JsonNode params) {
        URI uri = buildTemplateUri(config, phone, title, content, params);
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofMillis(intValue(config, "timeoutMs", 5000)))
            .GET();
        appendHeaders(config.get("headers"), builder);
        sendAndValidate(builder.build());
    }

    private URI buildTemplateUri(JsonNode config, String phone, String title, String content, JsonNode params) {
        String url = textValue(config, "url", "endpoint");
        if (!StringUtils.hasText(url)) {
            String baseUrl = defaultString(textValue(config, "baseUrl", "base_url"), "https://push.spug.cc/send");
            String templateId = textValue(config, "templateId", "template_id", "template");
            if (!StringUtils.hasText(templateId)) {
                throw new BusinessException(400, "Spug sms template mode requires templateId");
            }
            url = joinUrl(baseUrl, templateId.trim());
        }

        Map<String, String> query = new LinkedHashMap<>();
        appendQueryObject(config.get("query"), query);
        query.put(defaultString(textValue(config, "targetParam", "targetsParam"), "targets"), phone);
        appendParams(params, query);
        if (StringUtils.hasText(title)) {
            query.putIfAbsent(defaultString(textValue(config, "titleParam"), "title"), title);
        }
        if (StringUtils.hasText(content)) {
            query.putIfAbsent(defaultString(textValue(config, "contentParam"), "content"), content);
        }
        return URI.create(appendQuery(url, query));
    }

    private void sendXsendSms(JsonNode config, String phone, String title, String content, JsonNode params) {
        String url = textValue(config, "url", "endpoint");
        if (!StringUtils.hasText(url)) {
            String token = textValue(config, "token", "key");
            if (!StringUtils.hasText(token)) {
                throw new BusinessException(400, "Spug sms xsend mode requires url or token");
            }
            String baseUrl = defaultString(textValue(config, "baseUrl", "base_url"), "https://push.spug.cc/xsend");
            url = joinUrl(baseUrl, token.trim());
        }

        ObjectNode body = objectMapper.createObjectNode();
        appendBodyObject(config.get("body"), body);
        body.put(defaultString(textValue(config, "targetParam", "targetsParam"), "targets"), phone);
        body.put(defaultString(textValue(config, "channelParam"), "channel"), defaultString(textValue(config, "channel"), "sms"));
        if (StringUtils.hasText(title)) {
            body.put(defaultString(textValue(config, "titleParam"), "title"), title);
        }
        if (StringUtils.hasText(content)) {
            body.put(defaultString(textValue(config, "contentParam"), "content"), content);
        }
        appendParams(params, body);

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url.trim()))
            .timeout(Duration.ofMillis(intValue(config, "timeoutMs", 5000)))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(toJson(body), StandardCharsets.UTF_8));
        appendHeaders(config.get("headers"), builder);
        sendAndValidate(builder.build());
    }

    private void sendAndValidate(HttpRequest request) {
        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(500, "Spug sms request failed: HTTP " + response.statusCode());
            }
            validateResponseBody(response.body());
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(500, "Spug sms request failed: " + exception.getMessage());
        }
    }

    private void validateResponseBody(String body) {
        if (!StringUtils.hasText(body)) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            JsonNode success = node.get("success");
            if (success != null && success.isBoolean() && !success.asBoolean()) {
                throw new BusinessException(500, "Spug sms response failed: " + body);
            }
            JsonNode code = node.get("code");
            if (code != null && code.isNumber() && !List.of(0, 200).contains(code.asInt())) {
                throw new BusinessException(500, "Spug sms response failed: " + body);
            }
            JsonNode status = node.get("status");
            if (status != null && StringUtils.hasText(status.asText())) {
                String normalized = status.asText().trim().toLowerCase(Locale.ROOT);
                if (List.of("error", "failed", "fail").contains(normalized)) {
                    throw new BusinessException(500, "Spug sms response failed: " + body);
                }
            }
        } catch (JsonProcessingException ignored) {
            // Some push endpoints return plain text on success.
        }
    }

    private void ensureConfigObject(JsonNode config) {
        if (config == null || config.isNull() || !config.isObject()) {
            throw new BusinessException(400, "Sms carrier config must be a JSON object");
        }
    }

    private void appendQueryObject(JsonNode node, Map<String, String> target) {
        if (node == null || node.isNull() || !node.isObject()) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            if (!entry.getValue().isNull()) {
                target.put(entry.getKey(), nodeValueAsText(entry.getValue()));
            }
        }
    }

    private void appendBodyObject(JsonNode node, ObjectNode target) {
        if (node == null || node.isNull() || !node.isObject()) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            target.set(entry.getKey(), entry.getValue());
        }
    }

    private void appendParams(JsonNode params, Map<String, String> target) {
        if (params == null || params.isNull() || !params.isObject()) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = params.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            if (!entry.getValue().isNull()) {
                target.put(entry.getKey(), nodeValueAsText(entry.getValue()));
            }
        }
    }

    private void appendParams(JsonNode params, ObjectNode target) {
        if (params == null || params.isNull() || !params.isObject()) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = params.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            target.set(entry.getKey(), entry.getValue());
        }
    }

    private void appendHeaders(JsonNode headers, HttpRequest.Builder builder) {
        if (headers == null || headers.isNull() || !headers.isObject()) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = headers.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            if (!entry.getValue().isNull() && StringUtils.hasText(entry.getValue().asText())) {
                builder.header(entry.getKey(), entry.getValue().asText());
            }
        }
    }

    private String appendQuery(String url, Map<String, String> query) {
        if (query.isEmpty()) {
            return url.trim();
        }
        StringBuilder builder = new StringBuilder(url.trim());
        builder.append(url.contains("?") ? "&" : "?");
        boolean first = true;
        for (Map.Entry<String, String> entry : query.entrySet()) {
            if (!first) {
                builder.append("&");
            }
            first = false;
            builder.append(encode(entry.getKey())).append("=").append(encode(entry.getValue()));
        }
        return builder.toString();
    }

    private String joinUrl(String baseUrl, String segment) {
        String base = baseUrl.trim();
        String suffix = segment.trim();
        if (base.endsWith("/")) {
            return base + encode(suffix);
        }
        return base + "/" + encode(suffix);
    }

    private String textValue(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull() && StringUtils.hasText(value.asText())) {
                return value.asText();
            }
        }
        return null;
    }

    private int intValue(JsonNode node, String name, int fallback) {
        JsonNode value = node.get(name);
        if (value == null || value.isNull()) {
            return fallback;
        }
        if (value.isInt()) {
            return value.asInt();
        }
        try {
            return Integer.parseInt(value.asText());
        } catch (NumberFormatException exception) {
            throw new BusinessException(400, "Sms carrier config " + name + " must be a number");
        }
    }

    private String defaultString(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String nodeValueAsText(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        if (node.isObject() || node.isArray()) {
            return toJson(node);
        }
        return node.asText();
    }

    private String encode(String value) {
        return URLEncoder.encode(defaultString(value, ""), StandardCharsets.UTF_8);
    }

    private String toJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node == null ? NullNode.getInstance() : node);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(500, "Failed to serialize sms JSON payload");
        }
    }

    private JsonNode parseConfig(String value) {
        if (!StringUtils.hasText(value)) {
            return NullNode.getInstance();
        }
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(500, "Invalid sms carrier config stored");
        }
    }
}
