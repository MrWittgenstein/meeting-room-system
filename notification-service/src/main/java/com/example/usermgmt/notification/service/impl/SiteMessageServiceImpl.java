package com.example.usermgmt.notification.service.impl;

import com.example.usermgmt.common.exception.BusinessException;
import com.example.usermgmt.notification.config.NotificationFeatureProperties;
import com.example.usermgmt.notification.dto.MessageDtos;
import com.example.usermgmt.notification.dto.SiteMessageItemResponse;
import com.example.usermgmt.notification.dto.SiteMessageListResponse;
import com.example.usermgmt.notification.dto.SiteMessageReadRequest;
import com.example.usermgmt.notification.dto.SiteMessageReadResponse;
import com.example.usermgmt.notification.dto.SiteMessageSendRequest;
import com.example.usermgmt.notification.service.ReusableMessageService;
import com.example.usermgmt.notification.service.SiteMessageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SiteMessageServiceImpl implements SiteMessageService {

    private final JdbcTemplate jdbcTemplate;
    private final NotificationFeatureProperties featureProperties;
    private final ReusableMessageService reusableMessageService;

    @Override
    @Transactional
    public Integer send(SiteMessageSendRequest request) {
        ensureEnabled();
        List<String> userIds = normalizeUserIds(request.getUserIds());
        if (userIds.isEmpty()) {
            throw new BusinessException(400, "接收人不能为空");
        }

        int successCount = 0;
        for (String userId : userIds) {
            MessageDtos.SendRequest sendRequest = new MessageDtos.SendRequest();
            sendRequest.setBusinessCode(StringUtils.hasText(request.getCategory()) ? request.getCategory().trim() : "system");
            sendRequest.setTemplateCode(StringUtils.hasText(request.getTemplateCode()) ? request.getTemplateCode().trim() : "IN_APP_PLAIN_MESSAGE");
            MessageDtos.Recipient recipient = new MessageDtos.Recipient();
            recipient.setType("user_id");
            recipient.setValue(userId);
            sendRequest.setRecipient(recipient);
            sendRequest.setChannel("in_app");
            sendRequest.setParams(buildMessageParams(request));
            reusableMessageService.sendMessage(sendRequest, request.isDeferDispatch());
            successCount += 1;
        }
        return successCount;
    }

    @Override
    public SiteMessageListResponse list(String userId, Integer page, Integer pageSize, Boolean readStatus) {
        ensureEnabled();
        int normalizedPage = page == null || page < 1 ? 1 : page;
        int normalizedPageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        long offset = (long) (normalizedPage - 1) * normalizedPageSize;

        List<Object> countArgs = new ArrayList<>();
        String countSql = buildSiteMessageBaseSql(userId, readStatus, countArgs, true);
        Long total = jdbcTemplate.queryForObject(countSql, countArgs.toArray(), Long.class);

        List<Object> listArgs = new ArrayList<>();
        String listSql = buildSiteMessageBaseSql(userId, readStatus, listArgs, false) + " ORDER BY m.create_time DESC LIMIT ?, ?";
        listArgs.add(offset);
        listArgs.add(normalizedPageSize);
        List<SiteMessageItemResponse> items = jdbcTemplate.query(listSql, this::mapSiteMessageRow, listArgs.toArray());

        Long unreadCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM `message` WHERE channel_code = 'in_app' AND recipient_type = 'user_id' AND recipient_value = ? AND read_time IS NULL AND status = 'sent'",
            Long.class,
            userId
        );

        SiteMessageListResponse response = new SiteMessageListResponse();
        response.setTotal(total == null ? 0L : total);
        response.setPage(normalizedPage);
        response.setPageSize(normalizedPageSize);
        response.setUnreadCount(unreadCount == null ? 0L : unreadCount);
        response.setList(items);
        return response;
    }

    @Override
    public SiteMessageReadResponse markRead(String userId, SiteMessageReadRequest request) {
        ensureEnabled();
        if (request == null) {
            throw new BusinessException(400, "消息参数不能为空");
        }

        int updatedCount;
        if (Boolean.TRUE.equals(request.getMarkAll())) {
            updatedCount = jdbcTemplate.update(
                "UPDATE `message` SET read_time = NOW(), update_time = NOW() WHERE channel_code = 'in_app' AND recipient_type = 'user_id' AND recipient_value = ? AND read_time IS NULL",
                userId
            );
        } else {
            List<String> messageIds = normalizeMessageIds(request.getMessageIds());
            if (messageIds.isEmpty()) {
                throw new BusinessException(400, "消息ID不能为空");
            }
            String placeholders = String.join(",", messageIds.stream().map(id -> "?").toList());
            List<Object> args = new ArrayList<>();
            args.add(userId);
            args.addAll(messageIds);
            updatedCount = jdbcTemplate.update(
                "UPDATE `message` SET read_time = NOW(), update_time = NOW() WHERE channel_code = 'in_app' AND recipient_type = 'user_id' AND recipient_value = ? AND read_time IS NULL AND id IN (" + placeholders + ")",
                args.toArray()
            );
        }

        Long unreadCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM `message` WHERE channel_code = 'in_app' AND recipient_type = 'user_id' AND recipient_value = ? AND read_time IS NULL AND status = 'sent'",
            Long.class,
            userId
        );

        SiteMessageReadResponse response = new SiteMessageReadResponse();
        response.setUpdatedCount(updatedCount);
        response.setUnreadCount(unreadCount == null ? 0L : unreadCount);
        return response;
    }

    private JsonNode buildMessageParams(SiteMessageSendRequest request) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        if (request.getParams() != null && request.getParams().isObject()) {
            request.getParams().fields().forEachRemaining(entry -> node.set(entry.getKey(), entry.getValue()));
        }
        if (!node.hasNonNull("title")) {
            node.put("title", request.getTitle().trim());
        }
        if (!node.hasNonNull("body")) {
            node.put("body", request.getContent().trim());
        }
        if (!node.hasNonNull("link")) {
            node.put("link", StringUtils.hasText(request.getLink()) ? request.getLink().trim() : "");
        }
        return node;
    }

    private void ensureEnabled() {
        if (!featureProperties.isSiteMessageEnabled()) {
            throw new BusinessException(40001, "站内消息功能未启用");
        }
    }

    private String buildSiteMessageBaseSql(String userId, Boolean readStatus, List<Object> args, boolean countOnly) {
        StringBuilder sql = new StringBuilder();
        if (countOnly) {
            sql.append("SELECT COUNT(1) ");
        } else {
            sql.append(
                """
                SELECT m.id,
                       m.recipient_value AS user_id,
                       COALESCE(m.content_title, m.business_type) AS title,
                       m.content_body AS content,
                       m.business_type AS category,
                       JSON_UNQUOTE(JSON_EXTRACT(m.params, '$.link')) AS link,
                       CASE WHEN m.read_time IS NULL THEN 0 ELSE 1 END AS is_read,
                       m.created_by AS creator_id,
                       COALESCE(m.created_by, 'system') AS creator_name,
                       m.create_time,
                       m.read_time
                """
            );
        }
        sql.append("FROM `message` m ");
        sql.append("WHERE m.channel_code = 'in_app' AND m.recipient_type = 'user_id' AND m.status = 'sent' AND m.recipient_value = ? ");
        args.add(userId);
        if (readStatus != null) {
            sql.append(Boolean.TRUE.equals(readStatus) ? "AND m.read_time IS NOT NULL " : "AND m.read_time IS NULL ");
        }
        return sql.toString();
    }

    private SiteMessageItemResponse mapSiteMessageRow(ResultSet rs, int rowNum) throws SQLException {
        SiteMessageItemResponse response = new SiteMessageItemResponse();
        response.setMessageId(rs.getString("id"));
        response.setUserId(rs.getString("user_id"));
        response.setTitle(rs.getString("title"));
        response.setContent(rs.getString("content"));
        response.setCategory(rs.getString("category"));
        response.setLink(rs.getString("link"));
        response.setRead(rs.getInt("is_read") == 1);
        response.setCreatorId(rs.getString("creator_id"));
        response.setCreatorName(rs.getString("creator_name"));
        response.setCreatedAt(rs.getTimestamp("create_time") == null ? null : rs.getTimestamp("create_time").toLocalDateTime());
        response.setReadAt(rs.getTimestamp("read_time") == null ? null : rs.getTimestamp("read_time").toLocalDateTime());
        return response;
    }

    private List<String> normalizeUserIds(List<String> rawUserIds) {
        if (rawUserIds == null || rawUserIds.isEmpty()) {
            return List.of();
        }
        Set<String> uniqueIds = new LinkedHashSet<>();
        for (String userId : rawUserIds) {
            if (StringUtils.hasText(userId)) {
                uniqueIds.add(userId.trim());
            }
        }
        return new ArrayList<>(uniqueIds);
    }

    private List<String> normalizeMessageIds(List<String> rawMessageIds) {
        if (rawMessageIds == null || rawMessageIds.isEmpty()) {
            return List.of();
        }
        Set<String> uniqueIds = new LinkedHashSet<>();
        for (String messageId : rawMessageIds) {
            if (StringUtils.hasText(messageId)) {
                uniqueIds.add(messageId.trim());
            }
        }
        return new ArrayList<>(uniqueIds);
    }
}
