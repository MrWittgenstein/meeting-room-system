package com.example.usermgmt.notification.util;

import com.example.usermgmt.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TemplateRenderEngine {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\{?\\s*(.+?)\\s*\\}\\}\\}?");
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("^\\$sys\\.([a-zA-Z_][a-zA-Z0-9_]*)(?:\\((.*)\\))?$");
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*|\\[[0-9]+\\])*$");
    private static final List<String> RESERVED_VARIABLES = List.of("null", "true", "false", "undefined");

    private final ObjectMapper objectMapper;

    public TemplateRenderEngine(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RenderedContent render(JsonNode content, String channel, JsonNode params, Function<String, String> envLookup) {
        JsonNode effectiveContent = resolveChannelContent(content, channel);
        Map<String, Object> context = toMap(params);

        String rawTitle = resolveTextField(effectiveContent, "title", "subject");
        String rawBody = resolveBodyField(effectiveContent);
        String renderedTitle = rawTitle == null ? null : renderText(rawTitle, context, envLookup);
        String renderedBody = renderText(rawBody, context, envLookup);

        RenderedContent renderedContent = new RenderedContent();
        renderedContent.setTitle(renderedTitle);
        renderedContent.setBody(renderedBody);
        renderedContent.setChannel(channel);
        renderedContent.setSnapshot(effectiveContent == null ? NullNode.getInstance() : effectiveContent.deepCopy());
        return renderedContent;
    }

    public String renderText(String template, Map<String, Object> context, Function<String, String> envLookup) {
        if (template == null) {
            return "";
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String renderedValue = stringify(evaluateExpression(matcher.group(1), context, envLookup));
            matcher.appendReplacement(result, Matcher.quoteReplacement(renderedValue));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public void validateVariables(JsonNode paramsSchema) {
        if (paramsSchema == null || paramsSchema.isNull()) {
            return;
        }
        if (!paramsSchema.isObject()) {
            throw new BusinessException(400, "paramsSchema must be a JSON object");
        }
        paramsSchema.fieldNames().forEachRemaining(this::validateVariableName);
    }

    public void validateTemplateExpressions(JsonNode content) {
        if (content == null || content.isNull()) {
            throw new BusinessException(400, "content is required");
        }
        validateNode(content);
    }

    private void validateNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isTextual()) {
            validateTemplateText(node.asText());
            return;
        }
        if (node.isArray()) {
            node.forEach(this::validateNode);
            return;
        }
        if (node.isObject()) {
            node.elements().forEachRemaining(this::validateNode);
        }
    }

    private void validateTemplateText(String template) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        while (matcher.find()) {
            String expression = matcher.group(1).trim();
            String baseExpression = expression.split("\\|")[0].trim();
            if (baseExpression.startsWith("$sys.")) {
                validateSystemExpression(baseExpression);
            } else {
                validateVariableName(baseExpression);
            }
        }
    }

    private void validateSystemExpression(String expression) {
        Matcher matcher = FUNCTION_PATTERN.matcher(expression);
        if (!matcher.matches()) {
            throw new BusinessException(400, "Invalid system expression: " + expression);
        }
    }

    private void validateVariableName(String variableName) {
        if (!StringUtils.hasText(variableName)) {
            throw new BusinessException(400, "Variable name cannot be blank");
        }
        if (RESERVED_VARIABLES.contains(variableName)) {
            throw new BusinessException(400, "Reserved variable name is not allowed: " + variableName);
        }
        if (!VARIABLE_PATTERN.matcher(variableName).matches()) {
            throw new BusinessException(400, "Invalid variable name: " + variableName);
        }
    }

    private Object evaluateExpression(String expression, Map<String, Object> context, Function<String, String> envLookup) {
        String[] stages = expression.split("\\|");
        Object value = evaluateBase(stages[0].trim(), context, envLookup);
        for (int i = 1; i < stages.length; i += 1) {
            value = applyFilter(value, stages[i].trim());
        }
        return value;
    }

    private Object evaluateBase(String expression, Map<String, Object> context, Function<String, String> envLookup) {
        if (expression.startsWith("$sys.")) {
            return evaluateSystemExpression(expression, envLookup);
        }
        return resolvePath(context, expression);
    }

    private Object evaluateSystemExpression(String expression, Function<String, String> envLookup) {
        Matcher matcher = FUNCTION_PATTERN.matcher(expression);
        if (!matcher.matches()) {
            throw new BusinessException(400, "Invalid system function: " + expression);
        }
        String name = matcher.group(1);
        String arguments = matcher.group(2);
        return switch (name) {
            case "now" -> Instant.now().toEpochMilli();
            case "uuid" -> UUID.randomUUID().toString();
            case "random_int" -> randomInt(arguments);
            case "env" -> env(arguments, envLookup);
            default -> throw new BusinessException(400, "Unsupported system function: " + name);
        };
    }

    private Integer randomInt(String arguments) {
        List<String> args = parseArguments(arguments);
        if (args.size() != 2) {
            throw new BusinessException(400, "$sys.random_int requires 2 arguments");
        }
        int min = Integer.parseInt(stripQuotes(args.get(0)));
        int max = Integer.parseInt(stripQuotes(args.get(1)));
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private String env(String arguments, Function<String, String> envLookup) {
        List<String> args = parseArguments(arguments);
        if (args.size() != 1) {
            throw new BusinessException(400, "$sys.env requires 1 argument");
        }
        String key = stripQuotes(args.get(0));
        String value = envLookup.apply(key);
        return value == null ? "" : value;
    }

    private Object applyFilter(Object source, String filterExpression) {
        String[] tokens = filterExpression.split(":", 2);
        String filterName = tokens[0].trim();
        List<String> args = tokens.length > 1 ? parseArguments(tokens[1]) : List.of();
        return switch (filterName) {
            case "default" -> applyDefault(source, args);
            case "date" -> applyDate(source, args);
            case "currency" -> applyCurrency(source, args);
            case "upper" -> stringify(source).toUpperCase(Locale.ROOT);
            case "lower" -> stringify(source).toLowerCase(Locale.ROOT);
            case "truncate" -> applyTruncate(source, args);
            case "escape_html" -> escapeHtml(stringify(source));
            case "escape_json" -> escapeJson(stringify(source));
            case "substring" -> applySubstring(source, args);
            case "replace" -> applyReplace(source, args);
            default -> throw new BusinessException(400, "Unsupported filter: " + filterName);
        };
    }

    private Object applyDefault(Object source, List<String> args) {
        if (source == null || stringify(source).isBlank()) {
            return args.isEmpty() ? "" : stripQuotes(args.get(0));
        }
        return source;
    }

    private String applyDate(Object source, List<String> args) {
        if (source == null) {
            return "";
        }
        String pattern = args.isEmpty() ? "yyyy-MM-dd HH:mm:ss" : stripQuotes(args.get(0));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        if (source instanceof Number number) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(number.longValue()), ZoneId.systemDefault()).format(formatter);
        }
        String text = stringify(source);
        try {
            return LocalDateTime.parse(text).format(formatter);
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(text).atStartOfDay().format(formatter);
        } catch (Exception ignored) {
        }
        return text;
    }

    private String applyCurrency(Object source, List<String> args) {
        if (source == null) {
            return "";
        }
        BigDecimal amount = new BigDecimal(stringify(source));
        String code = args.isEmpty() ? "CNY" : stripQuotes(args.get(0));
        int scale = args.size() > 1 ? Integer.parseInt(stripQuotes(args.get(1))) : 2;
        DecimalFormat format = new DecimalFormat();
        format.setCurrency(Currency.getInstance(code));
        format.setMaximumFractionDigits(scale);
        format.setMinimumFractionDigits(scale);
        return format.format(amount);
    }

    private String applyTruncate(Object source, List<String> args) {
        String text = stringify(source);
        if (args.isEmpty()) {
            return text;
        }
        int length = Integer.parseInt(stripQuotes(args.get(0)));
        String suffix = args.size() > 1 ? stripQuotes(args.get(1)) : "...";
        if (text.length() <= length) {
            return text;
        }
        return text.substring(0, length) + suffix;
    }

    private String applySubstring(Object source, List<String> args) {
        String text = stringify(source);
        if (args.isEmpty()) {
            return text;
        }
        int start = Integer.parseInt(stripQuotes(args.get(0)));
        int end = args.size() > 1 ? Integer.parseInt(stripQuotes(args.get(1))) : text.length();
        start = Math.max(0, Math.min(start, text.length()));
        end = Math.max(start, Math.min(end, text.length()));
        return text.substring(start, end);
    }

    private String applyReplace(Object source, List<String> args) {
        String text = stringify(source);
        if (args.size() < 2) {
            return text;
        }
        return text.replace(stripQuotes(args.get(0)), stripQuotes(args.get(1)));
    }

    private JsonNode resolveChannelContent(JsonNode content, String channel) {
        if (content == null || content.isNull()) {
            throw new BusinessException(400, "content is required");
        }
        if (!content.isObject() || !StringUtils.hasText(channel)) {
            return content;
        }
        JsonNode channelNode = content.get(channel);
        return channelNode == null || channelNode.isNull() ? content : channelNode;
    }

    private String resolveTextField(JsonNode node, String... fieldNames) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode child = node.get(fieldName);
            if (child != null && child.isTextual()) {
                return child.asText();
            }
        }
        return null;
    }

    private String resolveBodyField(JsonNode node) {
        if (node == null || node.isNull()) {
            throw new BusinessException(400, "Template body is empty");
        }
        if (node.isTextual()) {
            return node.asText();
        }
        for (String fieldName : List.of("body", "content", "text", "html")) {
            JsonNode child = node.get(fieldName);
            if (child != null && child.isTextual()) {
                return child.asText();
            }
        }
        return node.toString();
    }

    private Map<String, Object> toMap(JsonNode params) {
        if (params == null || params.isNull()) {
            return new HashMap<>();
        }
        return objectMapper.convertValue(params, objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));
    }

    private Object resolvePath(Object value, String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }
        Object current = value;
        StringBuilder token = new StringBuilder();
        for (int i = 0; i < path.length(); i += 1) {
            char ch = path.charAt(i);
            if (ch == '.') {
                current = resolveToken(current, token.toString());
                token.setLength(0);
            } else if (ch == '[') {
                current = resolveToken(current, token.toString());
                token.setLength(0);
                int closeIndex = path.indexOf(']', i);
                int index = Integer.parseInt(path.substring(i + 1, closeIndex));
                current = resolveIndex(current, index);
                i = closeIndex;
            } else {
                token.append(ch);
            }
        }
        if (!token.isEmpty()) {
            current = resolveToken(current, token.toString());
        }
        return current;
    }

    private Object resolveToken(Object current, String token) {
        if (!StringUtils.hasText(token)) {
            return current;
        }
        if (current instanceof Map<?, ?> map) {
            return map.get(token);
        }
        if (current instanceof JsonNode jsonNode) {
            JsonNode child = jsonNode.get(token);
            return child == null || child.isNull() ? null : child;
        }
        return null;
    }

    private Object resolveIndex(Object current, int index) {
        if (current instanceof List<?> list) {
            return index >= 0 && index < list.size() ? list.get(index) : null;
        }
        if (current instanceof JsonNode jsonNode && jsonNode.isArray()) {
            JsonNode child = jsonNode.get(index);
            return child == null || child.isNull() ? null : child;
        }
        return null;
    }

    private List<String> parseArguments(String rawArguments) {
        if (!StringUtils.hasText(rawArguments)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        char quoteChar = 0;
        for (int i = 0; i < rawArguments.length(); i += 1) {
            char ch = rawArguments.charAt(i);
            if ((ch == '"' || ch == '\'') && (!quoted || quoteChar == ch)) {
                if (!quoted) {
                    quoted = true;
                    quoteChar = ch;
                } else {
                    quoted = false;
                }
                current.append(ch);
                continue;
            }
            if (ch == ',' && !quoted) {
                result.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        if (!current.isEmpty()) {
            result.add(current.toString().trim());
        }
        return result;
    }

    private String stripQuotes(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private String stringify(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof JsonNode jsonNode) {
            if (jsonNode.isValueNode()) {
                return jsonNode.asText();
            }
            return jsonNode.toString();
        }
        return String.valueOf(value);
    }

    private String escapeHtml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    private String escapeJson(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    @Data
    public static class RenderedContent {
        private String title;
        private String body;
        private String channel;
        private JsonNode snapshot;
    }
}
