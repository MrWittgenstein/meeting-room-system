package com.example.usermgmt.notification.service;

import com.example.usermgmt.common.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CarrierMailService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public void sendTextEmailWithDefaultCarrier(String to, String subject, String text) {
        sendTextEmail(loadDefaultEmailConfig(), to, subject, text);
    }

    public void sendTextEmail(JsonNode config, String to, String subject, String text) {
        JavaMailSenderImpl sender = buildMailSender(config);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(resolveFrom(config));
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        sender.send(message);
    }

    private JsonNode loadDefaultEmailConfig() {
        List<JsonNode> configs = jdbcTemplate.query(
            """
            SELECT config_json
            FROM `carrier`
            WHERE channel_type = 'email' AND status = 'active'
            ORDER BY is_default DESC, priority ASC, update_time DESC
            LIMIT 1
            """,
            (rs, rowNum) -> parseConfig(rs.getString("config_json"))
        );
        if (configs.isEmpty()) {
            throw new BusinessException(400, "No active email carrier configured");
        }
        return configs.get(0);
    }

    private JavaMailSenderImpl buildMailSender(JsonNode config) {
        ensureConfigObject(config);
        String host = textValue(config, "host", "smtpHost");
        if (!StringUtils.hasText(host)) {
            throw new BusinessException(400, "Email carrier config requires host");
        }

        String protocol = defaultString(textValue(config, "protocol"), "smtp");
        String username = textValue(config, "username", "user");
        String password = textValue(config, "password");
        String encoding = defaultString(textValue(config, "defaultEncoding", "default-encoding", "encoding"), "UTF-8");

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host.trim());
        sender.setPort(intValue(config, "port", "smtps".equalsIgnoreCase(protocol) ? 465 : 25));
        sender.setProtocol(protocol.trim());
        sender.setDefaultEncoding(encoding.trim());
        if (StringUtils.hasText(username)) {
            sender.setUsername(username.trim());
        }
        if (password != null) {
            sender.setPassword(password);
        }

        Properties properties = new Properties();
        appendProperties(config.get("properties"), properties, "");
        if (StringUtils.hasText(username) && !hasAuthProperty(properties, protocol)) {
            properties.put("mail." + protocol.toLowerCase() + ".auth", "true");
        }
        sender.setJavaMailProperties(properties);
        return sender;
    }

    private void ensureConfigObject(JsonNode config) {
        if (config == null || config.isNull() || !config.isObject()) {
            throw new BusinessException(400, "Email carrier config must be a JSON object");
        }
    }

    private String resolveFrom(JsonNode config) {
        String from = textValue(config, "from", "fromAddress", "mailFrom", "username");
        if (!StringUtils.hasText(from)) {
            throw new BusinessException(400, "Email carrier config requires from or username");
        }
        return from.trim();
    }

    private void appendProperties(JsonNode node, Properties properties, String prefix) {
        if (node == null || node.isNull()) {
            return;
        }
        if (!node.isObject()) {
            if (StringUtils.hasText(prefix)) {
                properties.put(prefix, node.asText());
            }
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = StringUtils.hasText(prefix) ? prefix + "." + entry.getKey() : entry.getKey();
            appendProperties(entry.getValue(), properties, key);
        }
    }

    private boolean hasAuthProperty(Properties properties, String protocol) {
        String normalizedProtocol = defaultString(protocol, "smtp").toLowerCase();
        return properties.containsKey("mail." + normalizedProtocol + ".auth")
            || properties.containsKey("mail.smtp.auth")
            || properties.containsKey("mail.smtps.auth");
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
            throw new BusinessException(400, "Email carrier config " + name + " must be a number");
        }
    }

    private String defaultString(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private JsonNode parseConfig(String value) {
        if (!StringUtils.hasText(value)) {
            return NullNode.getInstance();
        }
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(500, "Invalid email carrier config stored");
        }
    }
}
