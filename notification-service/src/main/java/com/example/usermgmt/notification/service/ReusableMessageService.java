package com.example.usermgmt.notification.service;

import com.example.usermgmt.common.accesslog.TraceContext;
import com.example.usermgmt.common.exception.BusinessException;
import com.example.usermgmt.notification.config.NotificationFeatureProperties;
import com.example.usermgmt.notification.dto.ChannelDtos;
import com.example.usermgmt.notification.dto.MessageDtos;
import com.example.usermgmt.notification.dto.PolicyDtos;
import com.example.usermgmt.notification.dto.TemplateDtos;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.example.usermgmt.notification.util.TemplateRenderEngine;
import com.example.usermgmt.notification.util.TemplateRenderEngine.RenderedContent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.NullNode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ReusableMessageService {

    private static final int MAX_BATCH_SIZE = 500;
    private static final long FEISHU_TOKEN_REFRESH_SAFETY_MILLIS = 60_000L;
    private static final String FEISHU_TOKEN_URL = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";
    private static final String FEISHU_SEND_MESSAGE_URL = "https://open.feishu.cn/open-apis/im/v1/messages";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().build();

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final CarrierMailService carrierMailService;
    private final NotificationFeatureProperties featureProperties;
    private final TemplateRenderEngine templateRenderEngine;
    private final Environment environment;
    private final Map<String, FeishuTenantToken> feishuTokenCache = new ConcurrentHashMap<>();

    @Transactional
    public TemplateDtos.Record createTemplate(TemplateDtos.CreateRequest request) {
        String templateId = generateId();
        String templateCode = StringUtils.hasText(request.getTemplateCode()) ? request.getTemplateCode().trim() : "TPL_" + shortId();
        ensureTemplateCodeAvailable(templateCode, null);
        String channel = normalizeChannel(request.getChannel());
        ensureReusableChannel(channel);
        String status = normalizeTemplateStatus(request.getStatus(), "draft");
        templateRenderEngine.validateVariables(request.getParamsSchema());
        templateRenderEngine.validateTemplateExpressions(request.getContent());

        jdbcTemplate.update(
            """
            INSERT INTO `template`
                (`id`, `template_code`, `business_code`, `template_name`, `channel_code`, `content`, `variable_definition`, `version`, `status`, `created_by`)
            VALUES
                (?, ?, ?, ?, ?, CAST(? AS JSON), CAST(? AS JSON), '1', ?, ?)
            """,
            templateId,
            templateCode,
            request.getBusinessCode().trim(),
            request.getName().trim(),
            channel,
            toJson(request.getContent()),
            toJsonOrNull(request.getParamsSchema()),
            status,
            defaultActor(request.getCreatedBy())
        );
        return getTemplate(templateId);
    }

    public TemplateDtos.Record getTemplate(String templateId) {
        return querySingle(
            """
            SELECT id, template_code, business_code, template_name AS name, channel_code AS channel,
                   content, variable_definition AS params_schema, version, status, created_by, create_time AS created_time, update_time
            FROM `template`
            WHERE id = ?
            """,
            this::mapTemplateRecord,
            templateId
        );
    }

    @Transactional
    public TemplateDtos.Record updateTemplate(String templateId, TemplateDtos.UpdateRequest request) {
        TemplateDtos.Record current = getTemplate(templateId);
        ensureTemplateNotArchived(current);
        String templateCode = StringUtils.hasText(request.getTemplateCode()) ? request.getTemplateCode().trim() : current.getTemplateCode();
        ensureTemplateCodeAvailable(templateCode, templateId);
        String businessCode = StringUtils.hasText(request.getBusinessCode()) ? request.getBusinessCode().trim() : current.getBusinessCode();
        String name = StringUtils.hasText(request.getName()) ? request.getName().trim() : current.getName();
        String channel = StringUtils.hasText(request.getChannel()) ? normalizeChannel(request.getChannel()) : current.getChannel();
        ensureReusableChannel(channel);
        JsonNode content = request.getContent() == null ? current.getContent() : request.getContent();
        JsonNode paramsSchema = request.getParamsSchema() == null ? current.getParamsSchema() : request.getParamsSchema();
        String status = normalizeTemplateStatus(request.getStatus(), "draft");
        int nextVersion = current.getVersion() + 1;

        templateRenderEngine.validateVariables(paramsSchema);
        templateRenderEngine.validateTemplateExpressions(content);

        jdbcTemplate.update(
            """
            UPDATE `template`
            SET template_code = ?, business_code = ?, template_name = ?, channel_code = ?, content = CAST(? AS JSON),
                variable_definition = CAST(? AS JSON), version = ?, status = ?, update_time = NOW()
            WHERE id = ? AND status <> 'archived'
            """,
            templateCode,
            businessCode,
            name,
            channel,
            toJson(content),
            toJsonOrNull(paramsSchema),
            nextVersion,
            status,
            templateId
        );
        return getTemplate(templateId);
    }

    @Transactional
    public void deleteTemplate(String templateId) {
        int affected = jdbcTemplate.update("UPDATE `template` SET status = 'archived', update_time = NOW() WHERE id = ? AND status <> 'archived'", templateId);
        if (affected == 0) {
            throw new BusinessException(404, "Template not found");
        }
    }

    @Transactional
    public TemplateDtos.Record publishTemplate(String templateId, TemplateDtos.PublishRequest request) {
        TemplateDtos.Record current = getTemplate(templateId);
        ensureTemplateNotArchived(current);
        if (request != null && request.getVersion() != null) {
            if (!request.getVersion().equals(current.getVersion())) {
                throw new BusinessException(400, "Only current template version can be published");
            }
        }
        jdbcTemplate.update("UPDATE `template` SET status = 'active', update_time = NOW() WHERE id = ? AND status <> 'archived'", templateId);
        return getTemplate(templateId);
    }

    public TemplateDtos.ListResponse listTemplates(Integer page, Integer size, String businessCode, String channel, String status) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (StringUtils.hasText(businessCode)) {
            where.append(" AND business_code LIKE ?");
            args.add("%" + businessCode.trim() + "%");
        }
        if (StringUtils.hasText(channel)) {
            String normalizedChannel = normalizeChannel(channel);
            ensureReusableChannel(normalizedChannel);
            where.append(" AND channel_code = ?");
            args.add(normalizedChannel);
        }
        if (StringUtils.hasText(status)) {
            String normalizedStatus = normalizeTemplateStatus(status, null);
            where.append(" AND status = ?");
            args.add(normalizedStatus);
        }

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM `template`" + where, Long.class, args.toArray());
        List<Object> listArgs = new ArrayList<>(args);
        listArgs.add((normalizedPage - 1) * normalizedSize);
        listArgs.add(normalizedSize);
        List<TemplateDtos.Record> records = jdbcTemplate.query(
            """
            SELECT id, template_code, business_code, template_name AS name, channel_code AS channel,
                   content, variable_definition AS params_schema, version, status, created_by, create_time AS created_time, update_time
            FROM `template`
            """
                + where + " ORDER BY update_time DESC LIMIT ?, ?",
            this::mapTemplateRecord,
            listArgs.toArray()
        );

        TemplateDtos.ListResponse response = new TemplateDtos.ListResponse();
        response.setTotal(total == null ? 0L : total);
        response.setPage(normalizedPage);
        response.setSize(normalizedSize);
        response.setRecords(records);
        return response;
    }

    public TemplateDtos.PreviewResponse previewTemplate(TemplateDtos.PreviewRequest request) {
        TemplateVersionSource source = resolveTemplateSource(request.getTemplateId(), request.getVersion());
        String effectiveChannel = source.channel();
        if (StringUtils.hasText(request.getChannel())) {
            String requestedChannel = normalizeChannel(request.getChannel());
            ensureTemplateSupportsChannel(source, requestedChannel);
            effectiveChannel = requestedChannel;
        }
        RenderedContent renderedContent = templateRenderEngine.render(source.content(), effectiveChannel, request.getParams(), environment::getProperty);
        TemplateDtos.PreviewResponse response = new TemplateDtos.PreviewResponse();
        response.setTitle(renderedContent.getTitle());
        response.setBody(renderedContent.getBody());
        response.setRendered((renderedContent.getTitle() == null ? "" : renderedContent.getTitle() + "\n") + renderedContent.getBody());
        response.setChannel(renderedContent.getChannel());
        return response;
    }

    public List<TemplateDtos.VersionRecord> listTemplateVersions(String templateId) {
        return List.of(toTemplateVersionRecord(getTemplate(templateId)));
    }

    public TemplateDtos.VersionRecord getTemplateVersion(String templateId, Integer version) {
        TemplateDtos.Record template = getTemplate(templateId);
        if (version != null && !version.equals(template.getVersion())) {
            throw new BusinessException(404, "Template version not found");
        }
        return toTemplateVersionRecord(template);
    }

    @Transactional
    public TemplateDtos.Record rollbackTemplate(String templateId, TemplateDtos.RollbackRequest request) {
        throw new BusinessException(400, "Template rollback is not available in the document schema");
    }

    @Transactional
    public PolicyDtos.Record createPolicy(PolicyDtos.CreateRequest request) {
        String policyId = generateId();
        String channelType = StringUtils.hasText(request.getChannelType()) ? normalizeChannel(request.getChannelType()) : null;
        if (StringUtils.hasText(channelType)) {
            ensureReusableChannel(channelType);
        }
        String status = normalizePolicyStatus(request.getStatus(), "active");
        String sendMode = normalizeSendMode(request.getSendMode());
        validateDynamicCondition(request.getDynamicCondition());
        validatePolicySchedule(sendMode, request.getSendTime(), request.getRecurringRule(), request.getRecurringEndTime());
        String templateId = blankToNull(request.getTemplateId());
        String templateCode = blankToNull(request.getTemplateCode());
        if (StringUtils.hasText(templateId) || StringUtils.hasText(templateCode)) {
            TemplateVersionSource templateSource = resolveTemplateSourceByBusiness(
                request.getBusinessCode(),
                templateId,
                templateCode,
                channelType
            );
            templateId = templateSource.templateId();
            templateCode = templateSource.templateCode();
            channelType = templateSource.channel();
        }
        Timestamp sendTime = toTimestampOrNull(request.getSendTime());
        Timestamp recurringEndTime = toTimestampOrNull(request.getRecurringEndTime());
        Timestamp nextFireTime = toTimestampOrNull(resolveNextFireTime(
            sendMode,
            request.getSendTime(),
            request.getNextFireTime(),
            request.getRecurringRule(),
            request.getRecurringEndTime()
        ));

        jdbcTemplate.update(
            """
            INSERT INTO `policy`
                (`id`, `policy_name`, `template_id`, `template_code`, `carrier_id`, `channel_type`, `business_code`, `dynamic_condition`,
                 `send_mode`, `send_time`, `recurring_rule`, `recurring_end_time`, `next_fire_time`, `last_fire_time`,
                 `priority`, `max_retry`, `remark`, `status`, `created_by`)
            VALUES
                (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            policyId,
            request.getPolicyName().trim(),
            templateId,
            templateCode,
            blankToNull(request.getCarrierId()),
            channelType,
            request.getBusinessCode().trim(),
            request.getDynamicCondition().trim(),
            sendMode,
            sendTime,
            blankToNull(request.getRecurringRule()),
            recurringEndTime,
            nextFireTime,
            null,
            defaultInt(request.getPriority(), 5),
            defaultInt(request.getMaxRetry(), 3),
            blankToNull(request.getRemark()),
            status,
            defaultActor(request.getCreatedBy())
        );
        return getPolicy(policyId);
    }

    public PolicyDtos.Record getPolicy(String policyId) {
        return querySingle(
            """
            SELECT id, policy_name, template_id, template_code, carrier_id, channel_type, business_code, dynamic_condition,
                   send_mode, send_time, recurring_rule, recurring_end_time, next_fire_time, last_fire_time,
                   priority, max_retry, remark, status, create_time, update_time
            FROM `policy`
            WHERE id = ?
            """,
            this::mapPolicyRecord,
            policyId
        );
    }

    @Transactional
    public PolicyDtos.Record updatePolicy(String policyId, PolicyDtos.UpdateRequest request) {
        PolicyDtos.Record current = getPolicy(policyId);
        String channelType = StringUtils.hasText(request.getChannelType()) ? normalizeChannel(request.getChannelType()) : current.getChannelType();
        if (StringUtils.hasText(channelType)) {
            ensureReusableChannel(channelType);
        }
        String businessCode = StringUtils.hasText(request.getBusinessCode()) ? request.getBusinessCode().trim() : current.getBusinessCode();
        String dynamicCondition = StringUtils.hasText(request.getDynamicCondition()) ? request.getDynamicCondition().trim() : current.getDynamicCondition();
        validateDynamicCondition(dynamicCondition);
        String sendMode = StringUtils.hasText(request.getSendMode()) ? normalizeSendMode(request.getSendMode()) : current.getSendMode();
        String status = normalizePolicyStatus(request.getStatus(), current.getStatus());
        String effectiveSendTime = StringUtils.hasText(request.getSendTime()) ? request.getSendTime() : current.getSendTime() == null ? null : current.getSendTime().toString();
        String effectiveRecurringRule = request.getRecurringRule() != null ? request.getRecurringRule().trim() : current.getRecurringRule();
        String effectiveRecurringEndTime = StringUtils.hasText(request.getRecurringEndTime()) ? request.getRecurringEndTime() : current.getRecurringEndTime() == null ? null : current.getRecurringEndTime().toString();
        validatePolicySchedule(sendMode, effectiveSendTime, effectiveRecurringRule, effectiveRecurringEndTime);
        String templateId = StringUtils.hasText(request.getTemplateId()) ? request.getTemplateId().trim() : current.getTemplateId();
        String templateCode = StringUtils.hasText(request.getTemplateCode()) ? request.getTemplateCode().trim() : current.getTemplateCode();
        if (StringUtils.hasText(templateId) || StringUtils.hasText(templateCode)) {
            TemplateVersionSource templateSource = resolveTemplateSourceByBusiness(businessCode, templateId, templateCode, channelType);
            templateId = templateSource.templateId();
            templateCode = templateSource.templateCode();
            channelType = templateSource.channel();
        }
        String nextFireTime = resolveNextFireTime(
            sendMode,
            effectiveSendTime,
            request.getNextFireTime(),
            effectiveRecurringRule,
            effectiveRecurringEndTime
        );

        jdbcTemplate.update(
            """
            UPDATE `policy`
            SET policy_name = ?, template_id = ?, template_code = ?, carrier_id = ?, channel_type = ?, business_code = ?,
                dynamic_condition = ?, send_mode = ?, send_time = ?, recurring_rule = ?, recurring_end_time = ?, next_fire_time = ?,
                priority = ?, max_retry = ?, remark = ?, status = ?, update_time = NOW()
            WHERE id = ?
            """,
            StringUtils.hasText(request.getPolicyName()) ? request.getPolicyName().trim() : current.getPolicyName(),
            blankToNull(templateId),
            blankToNull(templateCode),
            blankToNull(request.getCarrierId() != null ? request.getCarrierId().trim() : current.getCarrierId()),
            channelType,
            businessCode,
            dynamicCondition,
            sendMode,
            toTimestampOrNull(effectiveSendTime),
            blankToNull(effectiveRecurringRule),
            toTimestampOrNull(effectiveRecurringEndTime),
            toTimestampOrNull(nextFireTime),
            request.getPriority() == null ? current.getPriority() : request.getPriority(),
            request.getMaxRetry() == null ? current.getMaxRetry() : request.getMaxRetry(),
            blankToNull(request.getRemark() != null ? request.getRemark().trim() : current.getRemark()),
            status,
            policyId
        );
        return getPolicy(policyId);
    }

    @Transactional
    public void deletePolicy(String policyId) {
        int affected = jdbcTemplate.update("DELETE FROM `policy` WHERE id = ?", policyId);
        if (affected == 0) {
            throw new BusinessException(404, "Policy not found");
        }
    }

    public PolicyDtos.ListResponse listPolicies(Integer page, Integer size, String businessCode, String channelType, String status) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE COALESCE(JSON_UNQUOTE(JSON_EXTRACT(dynamic_condition, '$.scope')), '') <> 'message'");
        if (StringUtils.hasText(businessCode)) {
            where.append(" AND business_code = ?");
            args.add(businessCode.trim());
        }
        if (StringUtils.hasText(channelType)) {
            where.append(" AND channel_type = ?");
            args.add(normalizeChannel(channelType));
        }
        if (StringUtils.hasText(status)) {
            where.append(" AND status = ?");
            args.add(status.trim().toLowerCase(Locale.ROOT));
        }

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM `policy`" + where, Long.class, args.toArray());
        List<Object> listArgs = new ArrayList<>(args);
        listArgs.add((normalizedPage - 1) * normalizedSize);
        listArgs.add(normalizedSize);
        List<PolicyDtos.Record> records = jdbcTemplate.query(
            """
            SELECT id, policy_name, template_id, template_code, carrier_id, channel_type, business_code, dynamic_condition,
                   send_mode, send_time, recurring_rule, recurring_end_time, next_fire_time, last_fire_time,
                   priority, max_retry, remark, status, create_time, update_time
            FROM `policy`
            """ + where + " ORDER BY update_time DESC LIMIT ?, ?",
            this::mapPolicyRecord,
            listArgs.toArray()
        );

        PolicyDtos.ListResponse response = new PolicyDtos.ListResponse();
        response.setTotal(total == null ? 0L : total);
        response.setPage(normalizedPage);
        response.setSize(normalizedSize);
        response.setRecords(records);
        return response;
    }

    @Transactional
    public ChannelDtos.ConfigureResponse configureChannel(String channel, ChannelDtos.ConfigureRequest request) {
        String normalizedChannel = normalizeChannel(channel);
        String provider = request.getProvider().trim();
        String existingId = findChannelId(normalizedChannel, provider);
        String status = normalizeChannelStatus(request.getStatus());
        boolean defaultCarrier = resolveDefaultCarrierSelection(normalizedChannel, existingId, status, request.getDefaultCarrier());
        if (defaultCarrier) {
            clearDefaultCarriers(normalizedChannel);
        }
        if (existingId == null) {
            existingId = generateId();
            jdbcTemplate.update(
                """
                INSERT INTO `carrier`
                    (`id`, `channel_name`, `channel_type`, `channel_provider`, `config_json`, `priority`, `weight`, `max_retry`, `remark`, `status`, `is_default`)
                VALUES
                    (?, ?, ?, ?, CAST(? AS JSON), ?, ?, ?, ?, ?, ?)
                """,
                existingId,
                StringUtils.hasText(request.getChannelName()) ? request.getChannelName().trim() : normalizedChannel + "-" + provider,
                normalizedChannel,
                provider,
                toJson(request.getConfig()),
                defaultInt(request.getPriority(), 1),
                defaultInt(request.getWeight(), 100),
                defaultInt(request.getMaxRetry(), 3),
                blankToNull(request.getRemark()),
                status,
                defaultCarrier ? 1 : 0
            );
        } else {
            jdbcTemplate.update(
                """
                UPDATE `carrier`
                SET channel_name = ?, config_json = CAST(? AS JSON), priority = ?, weight = ?, max_retry = ?, remark = ?, status = ?, is_default = ?, update_time = NOW()
                WHERE id = ?
                """,
                StringUtils.hasText(request.getChannelName()) ? request.getChannelName().trim() : normalizedChannel + "-" + provider,
                toJson(request.getConfig()),
                defaultInt(request.getPriority(), 1),
                defaultInt(request.getWeight(), 100),
                defaultInt(request.getMaxRetry(), 3),
                blankToNull(request.getRemark()),
                status,
                defaultCarrier ? 1 : 0,
                existingId
            );
        }
        return getChannelById(existingId);
    }

    public ChannelDtos.StatusResponse channelStatus() {
        List<ChannelDtos.ConfigureResponse> carriers = jdbcTemplate.query(
            """
            SELECT id, channel_type, channel_name, channel_provider, config_json, priority, weight, max_retry, status, is_default, update_time
            FROM `carrier`
            ORDER BY channel_type ASC, CASE WHEN status = 'active' THEN 0 ELSE 1 END ASC, is_default DESC, priority ASC, update_time DESC
            """,
            (rs, rowNum) -> mapChannel(rs)
        );
        Map<String, ChannelDtos.RuntimeStatus> channelMap = new LinkedHashMap<>();
        for (ChannelDtos.ConfigureResponse carrier : carriers) {
            if (!channelMap.containsKey(carrier.getChannelType())) {
                ChannelDtos.RuntimeStatus status = new ChannelDtos.RuntimeStatus();
                status.setChannelId(carrier.getChannelId());
                status.setChannelType(carrier.getChannelType());
                status.setChannelName(carrier.getChannelName());
                status.setProvider(carrier.getProvider());
                status.setConfig(carrier.getConfig());
                status.setStatus(carrier.getStatus());
                status.setPriority(carrier.getPriority());
                status.setWeight(carrier.getWeight());
                status.setMaxRetry(carrier.getMaxRetry());
                status.setDefaultCarrier(carrier.getDefaultCarrier());
                status.setUpdatedAt(carrier.getUpdatedAt());
                status.setHealth(resolveChannelHealth(carrier.getChannelType(), carrier.getStatus()));
                channelMap.put(carrier.getChannelType(), status);
            }
        }
        ChannelDtos.StatusResponse response = new ChannelDtos.StatusResponse();
        response.setChannels(channelMap);
        response.setCarriers(carriers);
        return response;
    }

    @Transactional
    public void deleteChannel(String channelId) {
        ChannelDtos.ConfigureResponse channel = getChannelById(channelId);
        int references = countCarrierReferences(channelId);
        if (references > 0) {
            throw new BusinessException(400, "Carrier is referenced by templates, policies, tasks or messages; disable it instead");
        }
        if (Boolean.TRUE.equals(channel.getDefaultCarrier())) {
            Integer activeAlternatives = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `carrier` WHERE channel_type = ? AND id <> ? AND status = 'active'",
                Integer.class,
                channel.getChannelType(),
                channelId
            );
            if (activeAlternatives != null && activeAlternatives > 0) {
                throw new BusinessException(400, "Default carrier must be switched before deletion");
            }
        }
        int affected = jdbcTemplate.update("DELETE FROM `carrier` WHERE id = ?", channelId);
        if (affected == 0) {
            throw new BusinessException(404, "Channel provider not found");
        }
    }

    @Transactional
    public ChannelDtos.ConfigureResponse switchChannel(String channel, ChannelDtos.SwitchRequest request) {
        String normalizedChannel = normalizeChannel(channel);
        String provider = request.getProvider().trim();
        String targetId = findChannelId(normalizedChannel, provider);
        if (targetId == null) {
            throw new BusinessException(404, "Channel provider not found");
        }
        jdbcTemplate.update("UPDATE `carrier` SET status = 'disabled', is_default = 0, update_time = NOW() WHERE channel_type = ?", normalizedChannel);
        jdbcTemplate.update("UPDATE `carrier` SET status = 'active', is_default = 1, update_time = NOW() WHERE id = ?", targetId);
        return getChannelById(targetId);
    }

    @Transactional
    public MessageDtos.SendResponse sendMessage(MessageDtos.SendRequest request) {
        return sendMessage(request, false);
    }

    @Transactional
    public MessageDtos.SendResponse sendMessage(MessageDtos.SendRequest request, boolean deferDispatch) {
        MessageRequestContext context = buildMessageRequestContext(
            request.getPolicyId(),
            request.getBusinessCode(),
            request.getTemplateId(),
            request.getTemplateCode(),
            request.getChannel(),
            request.getParams(),
            request.getStrategy()
        );
        MessageDtos.Recipient recipient = request.getRecipient();
        if (recipient == null) {
            throw new BusinessException(400, "recipient is required");
        }

        LocalDateTime plannedAt = parseScheduleAt(request.getScheduleAt());
        String policyId = resolveMessagePolicyId(request.getPolicyId(), context, plannedAt, request.getScheduleAt(), request.getStrategy());
        String taskId = createTaskRow(
            policyId,
            null,
            context,
            List.of(recipient),
            plannedAt,
            request.getParams()
        );
        String messageId = createMessageRow(
            policyId,
            taskId,
            request.getRequestId(),
            null,
            context,
            normalizeRecipientType(recipient.getType()),
            recipient.getValue().trim(),
            plannedAt
        );

        if (!deferDispatch && !plannedAt.isAfter(LocalDateTime.now())) {
            dispatchMessageById(messageId);
        }
        return buildSendResponse(messageId);
    }

    @Transactional
    public MessageDtos.BatchSendResponse sendBatch(MessageDtos.BatchSendRequest request) {
        if (request.getRecipients().size() > MAX_BATCH_SIZE) {
            throw new BusinessException(400, "Batch size exceeds limit");
        }
        MessageRequestContext context = buildMessageRequestContext(
            request.getPolicyId(),
            request.getBusinessCode(),
            request.getTemplateId(),
            request.getTemplateCode(),
            request.getChannel(),
            request.getParams(),
            request.getStrategy()
        );
        LocalDateTime plannedAt = parseScheduleAt(request.getScheduleAt());
        String batchId = generateId();
        String taskId = createTaskRow(
            null,
            batchId,
            context,
            request.getRecipients(),
            plannedAt,
            request.getParams()
        );
        List<String> messageIds = new ArrayList<>();
        Set<String> uniqueRecipients = new LinkedHashSet<>();
        for (MessageDtos.Recipient recipient : request.getRecipients()) {
            String recipientType = normalizeRecipientType(recipient.getType());
            String recipientValue = recipient.getValue().trim();
            if (!uniqueRecipients.add(recipientType + ":" + recipientValue)) {
                continue;
            }
            String policyId = resolveMessagePolicyId(request.getPolicyId(), context, plannedAt, request.getScheduleAt(), request.getStrategy());
            messageIds.add(createMessageRow(policyId, taskId, request.getRequestId(), batchId, context, recipientType, recipientValue, plannedAt));
        }

        int success = 0;
        int failed = 0;
        if (!plannedAt.isAfter(LocalDateTime.now())) {
            for (String messageId : messageIds) {
                dispatchMessageById(messageId);
                String status = getMessage(messageId).getStatus();
                if ("sent".equals(status)) {
                    success += 1;
                } else {
                    failed += 1;
                }
            }
        }

        MessageDtos.BatchSendResponse response = new MessageDtos.BatchSendResponse();
        response.setBatchId(batchId);
        response.setTotal(messageIds.size());
        response.setSuccess(success);
        response.setFailed(failed);
        response.setStatus(plannedAt.isAfter(LocalDateTime.now()) ? "scheduled" : (failed == 0 ? "completed" : "partial_failed"));
        response.setMessageIds(messageIds);
        return response;
    }

    @Transactional
    public void cancelMessage(String messageId) {
        MessageDtos.MessageRecord record = getMessage(messageId);
        if (List.of("sent", "failed", "canceled").contains(record.getStatus())) {
            throw new BusinessException(400, "Message cannot be canceled in current status");
        }
        jdbcTemplate.update(
            "UPDATE `message` SET status = 'canceled', canceled = 1, update_time = NOW(), failure_reason = 'Canceled by operator' WHERE id = ?",
            messageId
        );
    }

    public MessageDtos.MessageRecord getMessage(String messageId) {
        return querySingle(
            """
            SELECT id, batch_id, request_id, policy_id, business_type AS business_code, template_id, template_code,
                   channel_code AS channel_type, provider, recipient_type, recipient_value, params AS params_json,
                   content_title, content_body, template_snapshot, response, status, retry_count, max_retry,
                   failure_reason, send_time_planned, send_time AS send_time_actual, next_retry_time,
                   create_time AS created_time, update_time
            FROM `message`
            WHERE id = ?
            """,
            this::mapMessageRecord,
            messageId
        );
    }

    public MessageDtos.BatchDetailResponse getBatch(String batchId) {
        List<MessageDtos.MessageRecord> records = jdbcTemplate.query(
            """
            SELECT id, batch_id, request_id, policy_id, business_type AS business_code, template_id, template_code,
                   channel_code AS channel_type, provider, recipient_type, recipient_value, params AS params_json,
                   content_title, content_body, template_snapshot, response, status, retry_count, max_retry,
                   failure_reason, send_time_planned, send_time AS send_time_actual, next_retry_time,
                   create_time AS created_time, update_time
            FROM `message`
            WHERE batch_id = ?
            ORDER BY create_time ASC
            """,
            this::mapMessageRecord,
            batchId
        );
        if (records.isEmpty()) {
            throw new BusinessException(404, "Batch not found");
        }
        int success = (int) records.stream().filter(item -> "sent".equals(item.getStatus())).count();
        int failed = (int) records.stream().filter(item -> "failed".equals(item.getStatus())).count();
        MessageDtos.BatchDetailResponse response = new MessageDtos.BatchDetailResponse();
        response.setBatchId(batchId);
        response.setTotal(records.size());
        response.setSuccess(success);
        response.setFailed(failed);
        response.setStatus(failed > 0 ? (success > 0 ? "partial_failed" : "failed") : "completed");
        response.setRecords(records);
        return response;
    }

    public MessageDtos.ListResponse listMessages(
        String recipientType,
        String recipient,
        String businessCode,
        String status,
        String startDate,
        String endDate,
        Integer page,
        Integer size
    ) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (StringUtils.hasText(recipientType)) {
            where.append(" AND recipient_type = ?");
            args.add(normalizeRecipientType(recipientType));
        }
        if (StringUtils.hasText(recipient)) {
            where.append(" AND recipient_value = ?");
            args.add(recipient.trim());
        }
        if (StringUtils.hasText(businessCode)) {
            where.append(" AND business_type = ?");
            args.add(businessCode.trim());
        }
        if (StringUtils.hasText(status)) {
            where.append(" AND status = ?");
            args.add(status.trim().toLowerCase(Locale.ROOT));
        }
        if (StringUtils.hasText(startDate)) {
            where.append(" AND create_time >= ?");
            args.add(Timestamp.valueOf(parseDateBoundary(startDate, true)));
        }
        if (StringUtils.hasText(endDate)) {
            where.append(" AND create_time <= ?");
            args.add(Timestamp.valueOf(parseDateBoundary(endDate, false)));
        }

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM `message`" + where, Long.class, args.toArray());
        List<Object> listArgs = new ArrayList<>(args);
        listArgs.add((normalizedPage - 1) * normalizedSize);
        listArgs.add(normalizedSize);
        List<MessageDtos.MessageRecord> records = jdbcTemplate.query(
            """
            SELECT id, batch_id, request_id, policy_id, business_type AS business_code, template_id, template_code,
                   channel_code AS channel_type, provider, recipient_type, recipient_value, params AS params_json,
                   content_title, content_body, template_snapshot, response, status, retry_count, max_retry,
                   failure_reason, send_time_planned, send_time AS send_time_actual, next_retry_time,
                   create_time AS created_time, update_time
            FROM `message`
            """ + where + " ORDER BY create_time DESC LIMIT ?, ?",
            this::mapMessageRecord,
            listArgs.toArray()
        );

        MessageDtos.ListResponse response = new MessageDtos.ListResponse();
        response.setTotal(total == null ? 0L : total);
        response.setPage(normalizedPage);
        response.setSize(normalizedSize);
        response.setRecords(records);
        return response;
    }

    @XxlJob("notificationMessageDispatch")
    @Transactional
    public void dispatchDueMessages() {
        List<String> dueMessageIds = jdbcTemplate.query(
            """
            SELECT id
            FROM `message`
            WHERE canceled = 0
              AND (
                    (status = 'pending' AND send_time_planned <= NOW())
                 OR (status = 'retrying' AND next_retry_time IS NOT NULL AND next_retry_time <= NOW() AND retry_count < max_retry)
              )
            ORDER BY send_time_planned ASC
            LIMIT 100
            """,
            (rs, rowNum) -> rs.getString("id")
        );
        for (String messageId : dueMessageIds) {
            dispatchMessageById(messageId);
        }
    }

    @XxlJob("notificationPolicyDispatch")
    @Transactional
    public void dispatchDuePolicies() {
        List<PolicyDtos.Record> policies = jdbcTemplate.query(
            """
            SELECT id, policy_name, template_id, template_code, carrier_id, channel_type, business_code, dynamic_condition,
                   send_mode, send_time, recurring_rule, recurring_end_time, next_fire_time, last_fire_time,
                   priority, max_retry, remark, status, create_time, update_time
            FROM `policy`
            WHERE status = 'active'
              AND (
                    (send_mode = 'immediate' AND last_fire_time IS NULL)
                 OR (send_mode = 'scheduled_once' AND send_time IS NOT NULL AND send_time <= NOW() AND last_fire_time IS NULL)
                 OR (send_mode = 'scheduled_recurring' AND next_fire_time IS NOT NULL AND next_fire_time <= NOW())
              )
            ORDER BY next_fire_time ASC
            LIMIT 50
            """,
            this::mapPolicyRecord
        );
        for (PolicyDtos.Record policy : policies) {
            executePolicy(policy);
        }
    }

    private MessageRequestContext buildMessageRequestContext(
        String policyId,
        String businessCode,
        String templateId,
        String templateCode,
        String requestedChannel,
        JsonNode params,
        MessageDtos.Strategy strategy
    ) {
        PolicyDtos.Record policy = null;
        if (StringUtils.hasText(policyId)) {
            policy = getPolicy(policyId.trim());
            ensurePolicyCanSend(policy);
            businessCode = resolvePolicyText(policy.getBusinessCode(), businessCode, "businessCode");
            templateId = resolvePolicyText(policy.getTemplateId(), templateId, "templateId");
            templateCode = resolvePolicyText(policy.getTemplateCode(), templateCode, "templateCode");
            requestedChannel = resolvePolicyChannel(policy.getChannelType(), requestedChannel);
        }
        if (!StringUtils.hasText(businessCode)) {
            throw new BusinessException(400, "businessCode is required when policyId is not provided");
        }
        TemplateVersionSource templateSource = resolveTemplateSourceByBusiness(businessCode, templateId, templateCode, requestedChannel);
        String channel = templateSource.channel();
        if (StringUtils.hasText(requestedChannel)) {
            String normalizedRequestedChannel = normalizeChannel(requestedChannel);
            ensureReusableChannel(normalizedRequestedChannel);
            ensureTemplateSupportsChannel(templateSource, normalizedRequestedChannel);
            channel = normalizedRequestedChannel;
        }
        ensureReusableChannel(channel);
        ChannelCarrier carrier = resolveCarrier(channel, policy == null ? null : policy.getCarrierId());
        RenderedContent renderedContent = templateRenderEngine.render(templateSource.content(), channel, params, environment::getProperty);

        MessageRequestContext context = new MessageRequestContext();
        context.businessCode = businessCode.trim();
        context.templateId = templateSource.templateId();
        context.templateCode = templateSource.templateCode();
        context.channel = channel;
        context.carrierId = carrier.id();
        context.provider = carrier.provider();
        context.maxRetry = strategy != null && strategy.getRetry() != null
            ? strategy.getRetry()
            : policy != null && policy.getMaxRetry() != null ? policy.getMaxRetry() : carrier.maxRetry();
        context.priority = strategy != null && strategy.getPriority() != null
            ? strategy.getPriority()
            : policy != null && policy.getPriority() != null ? policy.getPriority() : 5;
        context.params = params == null ? NullNode.getInstance() : params;
        context.renderedContent = renderedContent;
        return context;
    }

    private void ensurePolicyCanSend(PolicyDtos.Record policy) {
        if (!"active".equalsIgnoreCase(policy.getStatus())) {
            throw new BusinessException(400, "Policy is not active");
        }
        if (!StringUtils.hasText(policy.getBusinessCode())) {
            throw new BusinessException(400, "Policy businessCode is required");
        }
        if (!StringUtils.hasText(policy.getTemplateId()) && !StringUtils.hasText(policy.getTemplateCode())) {
            throw new BusinessException(400, "Policy template is required");
        }
        if (!StringUtils.hasText(policy.getChannelType())) {
            throw new BusinessException(400, "Policy channel is required");
        }
    }

    private String resolvePolicyText(String policyValue, String requestValue, String fieldName) {
        if (!StringUtils.hasText(policyValue)) {
            return requestValue;
        }
        if (StringUtils.hasText(requestValue) && !policyValue.trim().equals(requestValue.trim())) {
            throw new BusinessException(400, "Request " + fieldName + " does not match policy");
        }
        return policyValue.trim();
    }

    private String resolvePolicyChannel(String policyChannel, String requestChannel) {
        if (!StringUtils.hasText(policyChannel)) {
            return requestChannel;
        }
        String normalizedPolicyChannel = normalizeChannel(policyChannel);
        if (StringUtils.hasText(requestChannel)) {
            String normalizedRequestChannel = normalizeChannel(requestChannel);
            if (!normalizedPolicyChannel.equals(normalizedRequestChannel)) {
                throw new BusinessException(400, "Request channel does not match policy");
            }
        }
        return normalizedPolicyChannel;
    }

    private String resolveMessagePolicyId(
        String requestPolicyId,
        MessageRequestContext context,
        LocalDateTime plannedAt,
        String rawScheduleAt,
        MessageDtos.Strategy strategy
    ) {
        if (StringUtils.hasText(requestPolicyId)) {
            return requestPolicyId.trim();
        }
        String sendMode = StringUtils.hasText(rawScheduleAt) ? "scheduled_once" : "immediate";
        String policyId = generateId();
        String policyName = "MSG_" + shortId();
        jdbcTemplate.update(
            """
            INSERT INTO `policy`
                (`id`, `policy_name`, `template_id`, `template_code`, `carrier_id`, `channel_type`, `business_code`, `dynamic_condition`,
                 `send_mode`, `send_time`, `recurring_rule`, `recurring_end_time`, `next_fire_time`, `last_fire_time`,
                 `priority`, `max_retry`, `remark`, `status`, `created_by`)
            VALUES
                (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, ?, NULL, ?, ?, ?, 'active', 'system')
            """,
            policyId,
            policyName,
            context.templateId,
            context.templateCode,
            context.carrierId,
            context.channel,
            context.businessCode,
            "{\"scope\":\"message\"}",
            sendMode,
            "scheduled_once".equals(sendMode) ? Timestamp.valueOf(plannedAt) : null,
            "scheduled_once".equals(sendMode) ? Timestamp.valueOf(plannedAt) : null,
            strategy != null && strategy.getPriority() != null ? strategy.getPriority() : context.priority,
            strategy != null && strategy.getRetry() != null ? strategy.getRetry() : context.maxRetry,
            "message-owned policy"
        );
        return policyId;
    }

    private String createMessageRow(
        String policyId,
        String taskId,
        String requestId,
        String batchId,
        MessageRequestContext context,
        String recipientType,
        String recipientValue,
        LocalDateTime plannedAt
    ) {
        validateRecipient(recipientType, recipientValue);
        String messageId = generateId();
        jdbcTemplate.update(
            """
            INSERT INTO `message`
                (`id`, `task_id`, `batch_id`, `request_id`, `policy_id`, `template_id`, `template_code`, `carrier_id`, `channel_code`, `provider`,
                 `business_type`, `sender_uid`, `sender_contact`, `receiver_uid`, `receiver_contact`, `recipient_type`, `recipient_value`,
                 `params`, `content_title`, `content_body`, `template_snapshot`, `send_time_planned`, `status`, `retry_count`, `max_retry`, `created_by`)
            VALUES
                (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'system', NULL, ?, ?, ?, ?, CAST(? AS JSON), ?, ?, ?, ?, 'pending', 0, ?, 'system')
            """,
            messageId,
            blankToNull(taskId),
            batchId,
            blankToNull(requestId),
            blankToNull(policyId),
            context.templateId,
            context.templateCode,
            context.carrierId,
            context.channel,
            context.provider,
            context.businessCode,
            "user_id".equals(recipientType) ? recipientValue : "",
            recipientValue,
            recipientType,
            recipientValue,
            toJson(context.params),
            blankToNull(context.renderedContent.getTitle()),
            context.renderedContent.getBody(),
            toJson(context.renderedContent.getSnapshot()),
            Timestamp.valueOf(plannedAt),
            context.maxRetry
        );
        return messageId;
    }

    private String createTaskRow(
        String policyId,
        String batchId,
        MessageRequestContext context,
        List<MessageDtos.Recipient> recipients,
        LocalDateTime plannedAt,
        JsonNode params
    ) {
        String taskId = generateId();
        jdbcTemplate.update(
            """
            INSERT INTO `task`
                (`id`, `policy_id`, `batch_id`, `template_id`, `template_code`, `carrier_id`, `channel_code`, `business_type`,
                 `sender_uid`, `url`, `receiverlist`, `params`, `template_snapshot`, `send_time_planned`,
                 `max_retry`, `remark`, `status`)
            VALUES
                (?, ?, ?, ?, ?, ?, ?, ?, 'system', NULL, CAST(? AS JSON), CAST(? AS JSON), ?, ?, ?, ?, ?)
            """,
            taskId,
            blankToNull(policyId),
            blankToNull(batchId),
            context.templateId,
            context.templateCode,
            context.carrierId,
            context.channel,
            context.businessCode,
            toJson(buildReceiverList(recipients)),
            toJson(params == null ? context.params : params),
            toJson(context.renderedContent.getSnapshot()),
            Timestamp.valueOf(plannedAt),
            context.maxRetry,
            null,
            plannedAt.isAfter(LocalDateTime.now()) ? "waiting" : "executing"
        );
        return taskId;
    }

    private ArrayNode buildReceiverList(List<MessageDtos.Recipient> recipients) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        if (recipients == null) {
            return arrayNode;
        }
        for (MessageDtos.Recipient recipient : recipients) {
            if (recipient == null || !StringUtils.hasText(recipient.getValue())) {
                continue;
            }
            ObjectNode item = objectMapper.createObjectNode();
            item.put("type", StringUtils.hasText(recipient.getType()) ? normalizeRecipientType(recipient.getType()) : "user_id");
            item.put("value", recipient.getValue().trim());
            arrayNode.add(item);
        }
        return arrayNode;
    }

    @Transactional
    public void dispatchMessageById(String messageId) {
        MessageRow row = querySingle(
            """
            SELECT m.id, m.task_id, m.carrier_id, m.channel_code AS channel_type, m.provider, m.business_type AS business_code,
                   m.recipient_type, m.recipient_value, m.params AS params_json, m.content_title, m.content_body,
                   m.status, m.retry_count, m.max_retry, m.canceled, c.config_json AS carrier_config_json
            FROM `message` m
            LEFT JOIN `carrier` c ON c.id = m.carrier_id
            WHERE m.id = ?
            """,
            this::mapMessageRow,
            messageId
        );
        if (row.canceled() || List.of("sent", "canceled").contains(row.status())) {
            return;
        }

        jdbcTemplate.update("UPDATE `message` SET status = 'sending', update_time = NOW() WHERE id = ?", messageId);
        try {
            String channelResponse = sendThroughChannel(row);
            jdbcTemplate.update(
                "UPDATE `message` SET status = 'sent', send_time = NOW(), response = ?, failure_reason = NULL, next_retry_time = NULL, update_time = NOW() WHERE id = ?",
                channelResponse,
                messageId
            );
            updateTaskStatus(row.taskId(), "completed", null);
        } catch (Exception exception) {
            int nextRetryCount = row.retryCount() + 1;
            if (nextRetryCount < row.maxRetry()) {
                jdbcTemplate.update(
                    """
                    UPDATE `message`
                    SET status = 'retrying', retry_count = ?, response = ?, failure_reason = ?, next_retry_time = DATE_ADD(NOW(), INTERVAL 5 MINUTE), update_time = NOW()
                    WHERE id = ?
                    """,
                    nextRetryCount,
                    buildErrorResponse(exception.getMessage()),
                    shorten(exception.getMessage(), 500),
                    messageId
                );
                updateTaskStatus(row.taskId(), "waiting", shorten(exception.getMessage(), 255));
            } else {
                jdbcTemplate.update(
                    """
                    UPDATE `message`
                    SET status = 'failed', retry_count = ?, response = ?, failure_reason = ?, next_retry_time = NULL, update_time = NOW()
                    WHERE id = ?
                    """,
                    nextRetryCount,
                    buildErrorResponse(exception.getMessage()),
                    shorten(exception.getMessage(), 500),
                    messageId
                );
                updateTaskStatus(row.taskId(), "failed", shorten(exception.getMessage(), 255));
            }
        }
    }

    private void updateTaskStatus(String taskId, String status, String remark) {
        if (!StringUtils.hasText(taskId)) {
            return;
        }
        jdbcTemplate.update(
            "UPDATE `task` SET status = ?, remark = COALESCE(?, remark), update_time = NOW() WHERE id = ?",
            status,
            blankToNull(remark),
            taskId
        );
    }

    private String sendThroughChannel(MessageRow row) {
        String channel = normalizeChannel(row.channelType());
        ensureChannelEnabled(channel);
        return switch (channel) {
            case "in_app" -> sendInApp(row);
            case "email" -> sendEmail(row);
            case "sms" -> throw new BusinessException(400, "SMS is only supported by captcha verification");
            case "wechat" -> sendMockWechat(row);
            case "feishu" -> sendFeishu(row);
            default -> throw new BusinessException(400, "Unsupported channel: " + channel);
        };
    }

    private String sendInApp(MessageRow row) {
        if (!"user_id".equals(row.recipientType())) {
            throw new BusinessException(400, "in_app channel requires recipient.type = user_id");
        }
        return "{\"status\":\"sent\",\"channel\":\"in_app\"}";
    }

    private String sendEmail(MessageRow row) {
        if (!"email".equals(row.recipientType())) {
            throw new BusinessException(400, "email channel requires recipient.type = email");
        }
        carrierMailService.sendTextEmail(
            row.carrierConfig(),
            row.recipientValue(),
            StringUtils.hasText(row.contentTitle()) ? row.contentTitle() : row.businessCode(),
            row.contentBody()
        );
        return "{\"status\":\"sent\",\"channel\":\"email\"}";
    }

    private String sendMockWechat(MessageRow row) {
        if (!List.of("user_id", "phone", "email").contains(row.recipientType())) {
            throw new BusinessException(400, "wechat channel recipient type is unsupported");
        }
        return "{\"status\":\"sent\",\"channel\":\"wechat\",\"provider\":\"mock_wechat\"}";
    }

    private String sendFeishu(MessageRow row) {
        if (!List.of("user_id", "email", "feishu_open_id", "feishu_user_id", "feishu_union_id").contains(row.recipientType())) {
            throw new BusinessException(400, "feishu channel recipient type is unsupported");
        }
        JsonNode config = row.carrierConfig();
        String appId = requiredConfig(config, "appId", "feishu appId is required");
        String appSecret = requiredConfig(config, "appSecret", "feishu appSecret is required");
        String receiveIdType = resolveFeishuReceiveIdType(row, optionalConfig(config, "receiveIdType", "open_id"));
        String receiveId = resolveFeishuReceiveId(row, receiveIdType);
        String token = getFeishuTenantAccessToken(config, appId, appSecret);
        String msgType = normalizeFeishuMsgType(optionalConfig(config, "msgType", "interactive"));
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("receive_id", receiveId);
        payload.put("msg_type", msgType);
        payload.put("content", buildFeishuMessageContent(row, config, msgType));

        try {
            String sendUrl = optionalConfig(config, "sendMessageUrl", FEISHU_SEND_MESSAGE_URL)
                + "?receive_id_type=" + URLEncoder.encode(receiveIdType, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder(URI.create(sendUrl))
                .timeout(Duration.ofMillis(configInt(config, "timeoutMs", 5000)))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(payload), StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode responseNode = parseJsonNode(response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300 || responseNode.path("code").asInt(-1) != 0) {
                throw new BusinessException(500, "Feishu message send failed: " + shorten(response.body(), 500));
            }
            return response.body();
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(500, "Feishu message send failed: " + exception.getMessage());
        }
    }

    private String resolveFeishuReceiveIdType(MessageRow row, String configuredType) {
        return switch (row.recipientType()) {
            case "email" -> "email";
            case "feishu_open_id" -> "open_id";
            case "feishu_user_id" -> "user_id";
            case "feishu_union_id" -> "union_id";
            default -> StringUtils.hasText(configuredType) ? configuredType.trim().toLowerCase(Locale.ROOT) : "open_id";
        };
    }

    private String getFeishuTenantAccessToken(JsonNode config, String appId, String appSecret) {
        String cacheKey = appId;
        FeishuTenantToken cached = feishuTokenCache.get(cacheKey);
        long now = System.currentTimeMillis();
        if (cached != null && StringUtils.hasText(cached.token()) && cached.expiresAtMillis() - FEISHU_TOKEN_REFRESH_SAFETY_MILLIS > now) {
            return cached.token();
        }

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("app_id", appId);
        payload.put("app_secret", appSecret);
        String tokenUrl = optionalConfig(config, "tenantAccessTokenUrl", FEISHU_TOKEN_URL);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(tokenUrl))
                .timeout(Duration.ofMillis(configInt(config, "timeoutMs", 5000)))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(payload), StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode responseNode = parseJsonNode(response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300 || responseNode.path("code").asInt(-1) != 0) {
                throw new BusinessException(500, "Feishu token request failed: " + shorten(response.body(), 500));
            }
            String token = responseNode.path("tenant_access_token").asText(null);
            if (!StringUtils.hasText(token)) {
                throw new BusinessException(500, "Feishu token response missing tenant_access_token");
            }
            long expiresInSeconds = Math.max(60L, responseNode.path("expire").asLong(7200L));
            feishuTokenCache.put(cacheKey, new FeishuTenantToken(token, now + expiresInSeconds * 1000L));
            return token;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(500, "Feishu token request failed: " + exception.getMessage());
        }
    }

    private String resolveFeishuReceiveId(MessageRow row, String receiveIdType) {
        if (List.of("email", "feishu_open_id", "feishu_user_id", "feishu_union_id").contains(row.recipientType())) {
            return row.recipientValue();
        }
        if (!"user_id".equals(row.recipientType())) {
            return row.recipientValue();
        }
        JsonNode direct = row.params() == null ? null : row.params().get("feishuReceiveId");
        if (direct != null && direct.isTextual() && StringUtils.hasText(direct.asText())) {
            return direct.asText().trim();
        }
        FeishuIdentity identity = fetchFeishuIdentity(row.recipientValue());
        String normalizedType = receiveIdType == null ? "open_id" : receiveIdType.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedType) {
            case "user_id" -> requireText(identity.feishuUserId(), "User has no feishuUserId binding");
            case "union_id" -> requireText(identity.feishuUnionId(), "User has no feishuUnionId binding");
            case "email" -> requireText(identity.enterpriseEmail(), "User has no enterpriseEmail binding");
            case "open_id" -> requireText(identity.feishuOpenId(), "User has no feishuOpenId binding");
            default -> throw new BusinessException(400, "Unsupported feishu receiveIdType: " + receiveIdType);
        };
    }

    private FeishuIdentity fetchFeishuIdentity(String userId) {
        String userServiceUrl = environment.getProperty("notification.clients.user-service-url", "http://127.0.0.1:8181");
        String url = userServiceUrl + "/internal/users/" + URLEncoder.encode(userId, StandardCharsets.UTF_8) + "/feishu-identity";
        try {
            HttpRequest request = withTraceHeader(HttpRequest.newBuilder(URI.create(url)))
                .timeout(Duration.ofMillis(5000))
                .GET()
                .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode responseNode = parseJsonNode(response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300 || responseNode.path("code").asInt(-1) != 200) {
                throw new BusinessException(500, "Failed to resolve feishu identity: " + shorten(response.body(), 500));
            }
            JsonNode data = responseNode.path("data");
            return new FeishuIdentity(
                data.path("feishuOpenId").asText(null),
                data.path("feishuUserId").asText(null),
                data.path("feishuUnionId").asText(null),
                data.path("enterpriseEmail").asText(null)
            );
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(500, "Failed to resolve feishu identity: " + exception.getMessage());
        }
    }

    private String normalizeFeishuMsgType(String msgType) {
        if (!StringUtils.hasText(msgType)) {
            return "interactive";
        }
        String normalized = msgType.trim().toLowerCase(Locale.ROOT);
        if ("card".equals(normalized)) {
            return "interactive";
        }
        if (!List.of("text", "interactive").contains(normalized)) {
            throw new BusinessException(400, "Unsupported feishu msgType: " + msgType);
        }
        return normalized;
    }

    private String buildFeishuMessageContent(MessageRow row, JsonNode config, String msgType) {
        if ("interactive".equals(msgType)) {
            return buildFeishuCardContent(row, config);
        }
        return buildFeishuTextContent(row);
    }

    private String buildFeishuTextContent(MessageRow row) {
        ObjectNode content = objectMapper.createObjectNode();
        String text = StringUtils.hasText(row.contentTitle())
            ? row.contentTitle() + "\n" + row.contentBody()
            : row.contentBody();
        content.put("text", text == null ? "" : text);
        return toJson(content);
    }

    private String buildFeishuCardContent(MessageRow row, JsonNode config) {
        ObjectNode card = objectMapper.createObjectNode();
        ObjectNode cardConfig = card.putObject("config");
        cardConfig.put("wide_screen_mode", true);

        ObjectNode header = card.putObject("header");
        header.put("template", optionalConfig(config, "cardTemplate", "blue"));
        ObjectNode title = header.putObject("title");
        title.put("tag", "plain_text");
        title.put("content", StringUtils.hasText(row.contentTitle()) ? row.contentTitle() : "通知");

        ArrayNode elements = card.putArray("elements");
        ObjectNode div = elements.addObject();
        div.put("tag", "div");
        ObjectNode text = div.putObject("text");
        text.put("tag", "lark_md");
        text.put("content", normalizeFeishuCardText(row.contentBody()));
        return toJson(card);
    }

    private String normalizeFeishuCardText(String content) {
        if (!StringUtils.hasText(content)) {
            return "-";
        }
        return content.replace("\r\n", "\n").replace('\r', '\n');
    }

    private String requiredConfig(JsonNode config, String key, String message) {
        String value = optionalConfig(config, key, null);
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(400, message);
        }
        return value;
    }

    private String optionalConfig(JsonNode config, String key, String fallback) {
        if (config != null && config.isObject()) {
            JsonNode value = config.get(key);
            if (value != null && !value.isNull() && StringUtils.hasText(value.asText())) {
                return value.asText().trim();
            }
        }
        return fallback;
    }

    private int configInt(JsonNode config, String key, int fallback) {
        if (config != null && config.isObject()) {
            JsonNode value = config.get(key);
            if (value != null && value.canConvertToInt()) {
                return value.asInt();
            }
        }
        return fallback;
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(400, message);
        }
        return value.trim();
    }

    private MessageDtos.SendResponse buildSendResponse(String messageId) {
        MessageDtos.MessageRecord record = getMessage(messageId);
        MessageDtos.SendResponse response = new MessageDtos.SendResponse();
        response.setMessageId(record.getMessageId());
        response.setBatchId(record.getBatchId());
        response.setStatus(record.getStatus());
        response.setPlannedAt(record.getPlannedAt());
        response.setRetryCount(record.getRetryCount());
        return response;
    }

    private void executePolicy(PolicyDtos.Record policy) {
        try {
            List<MessageDtos.Recipient> recipients = resolvePolicyRecipients(policy);
            if (recipients.isEmpty()) {
                updatePolicyExecutionState(policy, "No recipients resolved");
                return;
            }

            MessageDtos.BatchSendRequest request = new MessageDtos.BatchSendRequest();
            request.setRequestId("policy-" + policy.getPolicyId() + "-" + System.currentTimeMillis());
            request.setBusinessCode(policy.getBusinessCode());
            request.setTemplateId(policy.getTemplateId());
            request.setTemplateCode(policy.getTemplateCode());
            request.setChannel(policy.getChannelType());
            request.setRecipients(recipients);
            request.setParams(resolvePolicyParams(policy));
            MessageDtos.Strategy strategy = new MessageDtos.Strategy();
            strategy.setPriority(policy.getPriority());
            strategy.setRetry(policy.getMaxRetry());
            request.setStrategy(strategy);
            request.setPolicyId(policy.getPolicyId());
            sendBatch(request);
            updatePolicyExecutionState(policy, null);
        } catch (Exception exception) {
            jdbcTemplate.update(
                """
                UPDATE `policy`
                SET last_fire_time = NOW(), next_fire_time = ?, update_time = NOW(), remark = ?
                WHERE id = ?
                """,
                toTimestampOrNull(resolvePolicyNextFireTimeAfterFire(policy)),
                shorten(exception.getMessage(), 255),
                policy.getPolicyId()
            );
        }
    }

    private void updatePolicyExecutionState(PolicyDtos.Record policy, String remark) {
        String nextFire = resolvePolicyNextFireTimeAfterFire(policy);
        jdbcTemplate.update(
            """
            UPDATE `policy`
            SET last_fire_time = NOW(), next_fire_time = ?, update_time = NOW(), remark = COALESCE(?, remark)
            WHERE id = ?
            """,
            toTimestampOrNull(nextFire),
            blankToNull(remark),
            policy.getPolicyId()
        );
    }

    private List<MessageDtos.Recipient> resolvePolicyRecipients(PolicyDtos.Record policy) throws Exception {
        JsonNode condition = objectMapper.readTree(policy.getDynamicCondition());
        if (condition.has("recipients")) {
            return extractRecipientsFromDynamicResponse(condition.get("recipients"), policy.getChannelType());
        }
        String api = condition.path("api").asText(null);
        if (!StringUtils.hasText(api)) {
            throw new BusinessException(400, "Policy dynamicCondition.api or recipients is required");
        }
        ObjectNode resolvedParams = resolvePolicyConditionParams(condition);
        JsonNode responseNode = invokeDynamicConditionApi(api.trim(), resolvedParams);
        return extractRecipientsFromDynamicResponse(responseNode, policy.getChannelType());
    }

    private JsonNode resolvePolicyParams(PolicyDtos.Record policy) throws JsonProcessingException {
        JsonNode condition = objectMapper.readTree(policy.getDynamicCondition());
        return resolvePolicyConditionParams(condition);
    }

    private ObjectNode resolvePolicyConditionParams(JsonNode condition) {
        JsonNode paramsNode = condition.path("params");
        JsonNode paramValuesNode = condition.path("param_values");
        ObjectNode resolvedParams = objectMapper.createObjectNode();
        if (paramsNode.isObject()) {
            paramsNode.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                if (value == null || value.isNull()) {
                    resolvedParams.putNull(entry.getKey());
                    return;
                }
                if (value.isTextual()) {
                    String rendered = templateRenderEngine.renderText(value.asText(""), jsonNodeToContextMap(paramValuesNode), environment::getProperty);
                    resolvedParams.put(entry.getKey(), rendered);
                    return;
                }
                resolvedParams.set(entry.getKey(), value);
            });
        }
        return resolvedParams;
    }

    private JsonNode invokeDynamicConditionApi(String api, JsonNode resolvedParams) throws Exception {
        URI uri = buildDynamicUri(api, resolvedParams);
        HttpRequest request = withTraceHeader(HttpRequest.newBuilder(uri)).GET().build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BusinessException(500, "Dynamic condition API failed: " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }

    private URI buildDynamicUri(String api, JsonNode resolvedParams) {
        String base = api.startsWith("http://") || api.startsWith("https://")
            ? api
            : environment.getProperty("notification.clients.dynamic-api-base-url", "http://127.0.0.1:8080") + api;
        if (resolvedParams == null || resolvedParams.isNull() || !resolvedParams.isObject() || resolvedParams.isEmpty()) {
            return URI.create(base);
        }
        StringBuilder query = new StringBuilder();
        resolvedParams.fields().forEachRemaining(entry -> {
            if (query.length() > 0) {
                query.append('&');
            }
            query.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            query.append('=');
            query.append(URLEncoder.encode(entry.getValue().asText(""), StandardCharsets.UTF_8));
        });
        return URI.create(base + (base.contains("?") ? "&" : "?") + query);
    }

    private HttpRequest.Builder withTraceHeader(HttpRequest.Builder builder) {
        String traceId = TraceContext.getTraceId();
        if (StringUtils.hasText(traceId)) {
            builder.header(TraceContext.TRACE_ID_HEADER, traceId);
        }
        return builder;
    }

    private List<MessageDtos.Recipient> extractRecipientsFromDynamicResponse(JsonNode responseNode, String channelType) {
        JsonNode dataNode = responseNode;
        if (responseNode.has("data")) {
            dataNode = responseNode.get("data");
        }
        if (dataNode != null && dataNode.isObject()) {
            for (String key : List.of("list", "records", "items", "ids", "userIds", "receiverIds")) {
                if (dataNode.has(key)) {
                    dataNode = dataNode.get(key);
                    break;
                }
            }
        }

        List<MessageDtos.Recipient> recipients = new ArrayList<>();
        if (dataNode instanceof ArrayNode arrayNode) {
            for (JsonNode item : arrayNode) {
                MessageDtos.Recipient recipient = toRecipient(item, channelType);
                if (recipient != null) {
                    recipients.add(recipient);
                }
            }
        } else {
            MessageDtos.Recipient recipient = toRecipient(dataNode, channelType);
            if (recipient != null) {
                recipients.add(recipient);
            }
        }
        return recipients;
    }

    private MessageDtos.Recipient toRecipient(JsonNode item, String channelType) {
        if (item == null || item.isNull()) {
            return null;
        }
        MessageDtos.Recipient recipient = new MessageDtos.Recipient();
        String normalizedChannel = StringUtils.hasText(channelType) ? normalizeChannel(channelType) : "in_app";
        if (item.isTextual() || item.isNumber()) {
            recipient.setType(defaultRecipientTypeByChannel(normalizedChannel));
            recipient.setValue(item.asText());
            return recipient;
        }
        if (item.isObject()) {
            if (item.hasNonNull("type") && item.hasNonNull("value")) {
                recipient.setType(normalizeRecipientType(item.get("type").asText()));
                recipient.setValue(item.get("value").asText());
                return recipient;
            }
            for (String key : List.of("userId", "uid", "user_id")) {
                if (item.hasNonNull(key)) {
                    recipient.setType("user_id");
                    recipient.setValue(item.get(key).asText());
                    return recipient;
                }
            }
            if (item.hasNonNull("phone")) {
                recipient.setType("phone");
                recipient.setValue(item.get("phone").asText());
                return recipient;
            }
            if (item.hasNonNull("email")) {
                recipient.setType("email");
                recipient.setValue(item.get("email").asText());
                return recipient;
            }
            if (item.hasNonNull("contact")) {
                recipient.setType(defaultRecipientTypeByChannel(normalizedChannel));
                recipient.setValue(item.get("contact").asText());
                return recipient;
            }
        }
        return null;
    }

    private String defaultRecipientTypeByChannel(String channelType) {
        return switch (normalizeChannel(channelType)) {
            case "email" -> "email";
            default -> "user_id";
        };
    }

    private TemplateVersionSource resolveTemplateSourceByBusiness(String businessCode, String templateId, String templateCode, String requestedChannel) {
        if (StringUtils.hasText(templateId)) {
            TemplateDtos.Record template = getTemplate(templateId.trim());
            ensureTemplateActive(template);
            return new TemplateVersionSource(template.getTemplateId(), template.getTemplateCode(), template.getBusinessCode(), template.getChannel(), template.getContent(), template.getParamsSchema());
        }
        if (StringUtils.hasText(templateCode)) {
            TemplateDtos.Record template = querySingle(
                """
                SELECT id, template_code, business_code, template_name AS name, channel_code AS channel,
                       content, variable_definition AS params_schema, version, status, created_by, create_time AS created_time, update_time
                FROM `template`
                WHERE template_code = ? AND status <> 'archived'
                """,
                this::mapTemplateRecord,
                templateCode.trim()
            );
            ensureTemplateActive(template);
            return new TemplateVersionSource(template.getTemplateId(), template.getTemplateCode(), template.getBusinessCode(), template.getChannel(), template.getContent(), template.getParamsSchema());
        }
        String channel = StringUtils.hasText(requestedChannel) ? normalizeChannel(requestedChannel) : null;
        if (StringUtils.hasText(channel)) {
            ensureReusableChannel(channel);
        }
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            """
            SELECT id, template_code, business_code, template_name AS name, channel_code AS channel,
                   content, variable_definition AS params_schema, version, status, created_by, create_time AS created_time, update_time
            FROM `template`
            WHERE business_code = ? AND status = 'active'
            """
        );
        args.add(businessCode.trim());
        if (StringUtils.hasText(channel)) {
            sql.append(" AND channel_code = ?");
            args.add(normalizeChannel(channel));
        }
        sql.append(" ORDER BY update_time DESC LIMIT 1");
        TemplateDtos.Record template = querySingle(sql.toString(), this::mapTemplateRecord, args.toArray());
        return new TemplateVersionSource(template.getTemplateId(), template.getTemplateCode(), template.getBusinessCode(), template.getChannel(), template.getContent(), template.getParamsSchema());
    }

    private void ensureTemplateActive(TemplateDtos.Record template) {
        if (!"active".equalsIgnoreCase(template.getStatus())) {
            throw new BusinessException(400, "Template is not active");
        }
    }

    private void ensureTemplateNotArchived(TemplateDtos.Record template) {
        if ("archived".equalsIgnoreCase(template.getStatus())) {
            throw new BusinessException(400, "Template is archived");
        }
    }

    private void ensureTemplateSupportsChannel(TemplateVersionSource templateSource, String requestedChannel) {
        if (!templateSource.channel().equalsIgnoreCase(requestedChannel)) {
            throw new BusinessException(400, "Template channel does not match requested channel");
        }
    }

    private void ensureReusableChannel(String channel) {
        if ("sms".equalsIgnoreCase(channel)) {
            throw new BusinessException(400, "SMS is only supported by captcha verification, not reusable message templates");
        }
        if ("push".equalsIgnoreCase(channel)) {
            throw new BusinessException(400, "Push channel has been replaced by feishu");
        }
    }

    private TemplateVersionSource resolveTemplateSource(String templateId, Integer version) {
        if (version == null) {
            TemplateDtos.Record template = getTemplate(templateId);
            return new TemplateVersionSource(template.getTemplateId(), template.getTemplateCode(), template.getBusinessCode(), template.getChannel(), template.getContent(), template.getParamsSchema());
        }
        TemplateDtos.VersionRecord record = getTemplateVersion(templateId, version);
        return new TemplateVersionSource(record.getTemplateId(), record.getTemplateCode(), record.getBusinessCode(), record.getChannel(), record.getContent(), record.getParamsSchema());
    }

    private void ensureTemplateCodeAvailable(String templateCode, String excludeTemplateId) {
        List<String> ids = jdbcTemplate.queryForList(
            "SELECT id FROM `template` WHERE template_code = ?",
            String.class,
            templateCode
        );
        if (!ids.isEmpty() && (excludeTemplateId == null || ids.stream().anyMatch(id -> !excludeTemplateId.equals(id)))) {
            throw new BusinessException(400, "templateCode already exists");
        }
    }

    private String findChannelId(String channelType, String provider) {
        List<String> ids = jdbcTemplate.queryForList(
            "SELECT id FROM `carrier` WHERE channel_type = ? AND channel_provider = ?",
            String.class,
            channelType,
            provider
        );
        return ids.isEmpty() ? null : ids.get(0);
    }

    private boolean resolveDefaultCarrierSelection(String channelType, String carrierId, String status, Boolean requestedDefault) {
        if (Boolean.TRUE.equals(requestedDefault) && !"active".equals(status)) {
            throw new BusinessException(400, "defaultCarrier requires active status");
        }
        if (requestedDefault != null) {
            return requestedDefault;
        }
        if (!"active".equals(status)) {
            return false;
        }
        if (carrierId != null && isDefaultCarrier(carrierId)) {
            return true;
        }
        return !hasDefaultCarrier(channelType);
    }

    private boolean isDefaultCarrier(String carrierId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM `carrier` WHERE id = ? AND is_default = 1",
            Integer.class,
            carrierId
        );
        return count != null && count > 0;
    }

    private boolean hasDefaultCarrier(String channelType) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM `carrier` WHERE channel_type = ? AND status = 'active' AND is_default = 1",
            Integer.class,
            channelType
        );
        return count != null && count > 0;
    }

    private void clearDefaultCarriers(String channelType) {
        jdbcTemplate.update("UPDATE `carrier` SET is_default = 0, update_time = NOW() WHERE channel_type = ? AND is_default = 1", channelType);
    }

    private ChannelCarrier resolveActiveCarrier(String channel) {
        List<ChannelCarrier> carriers = jdbcTemplate.query(
            """
            SELECT id, channel_provider, max_retry
            FROM `carrier`
            WHERE channel_type = ? AND status = 'active'
            ORDER BY is_default DESC, priority ASC, update_time DESC
            LIMIT 1
            """,
            (rs, rowNum) -> new ChannelCarrier(rs.getString("id"), rs.getString("channel_provider"), rs.getInt("max_retry")),
            channel
        );
        if (carriers.isEmpty()) {
            throw new BusinessException(400, "No active carrier configured for channel: " + channel);
        }
        return carriers.get(0);
    }

    private ChannelCarrier resolveCarrier(String channel, String carrierId) {
        if (!StringUtils.hasText(carrierId)) {
            return resolveActiveCarrier(channel);
        }
        List<ChannelCarrier> carriers = jdbcTemplate.query(
            """
            SELECT id, channel_provider, max_retry
            FROM `carrier`
            WHERE id = ? AND channel_type = ? AND status = 'active'
            LIMIT 1
            """,
            (rs, rowNum) -> new ChannelCarrier(rs.getString("id"), rs.getString("channel_provider"), rs.getInt("max_retry")),
            carrierId.trim(),
            channel
        );
        if (carriers.isEmpty()) {
            throw new BusinessException(400, "Policy carrier is not active for channel: " + channel);
        }
        return carriers.get(0);
    }

    private ChannelDtos.ConfigureResponse getChannelById(String channelId) {
        return querySingle(
            """
            SELECT id, channel_type, channel_name, channel_provider, config_json, priority, weight, max_retry, status, is_default, update_time
            FROM `carrier`
            WHERE id = ?
            """,
            (rs, rowNum) -> mapChannel(rs),
            channelId
        );
    }

    private int countCarrierReferences(String channelId) {
        Integer references = jdbcTemplate.queryForObject(
            """
            SELECT
                (SELECT COUNT(*) FROM `template` WHERE carrier_id = ?) +
                (SELECT COUNT(*) FROM `policy` WHERE carrier_id = ?) +
                (SELECT COUNT(*) FROM `task` WHERE carrier_id = ?) +
                (SELECT COUNT(*) FROM `message` WHERE carrier_id = ?)
            """,
            Integer.class,
            channelId,
            channelId,
            channelId,
            channelId
        );
        return references == null ? 0 : references;
    }

    private ChannelDtos.ConfigureResponse mapChannel(ResultSet rs) throws SQLException {
        ChannelDtos.ConfigureResponse response = new ChannelDtos.ConfigureResponse();
        response.setChannelId(rs.getString("id"));
        response.setChannelType(rs.getString("channel_type"));
        response.setChannelName(rs.getString("channel_name"));
        response.setProvider(rs.getString("channel_provider"));
        response.setConfig(parseJsonNode(rs.getString("config_json")));
        response.setPriority(rs.getInt("priority"));
        response.setWeight(rs.getInt("weight"));
        response.setMaxRetry(rs.getInt("max_retry"));
        response.setStatus(rs.getString("status"));
        response.setDefaultCarrier(rs.getInt("is_default") == 1);
        response.setHealth(resolveChannelHealth(response.getChannelType(), response.getStatus()));
        response.setUpdatedAt(toLocalDateTime(rs.getTimestamp("update_time")));
        return response;
    }

    private String resolveChannelHealth(String channelType, String status) {
        if (!"active".equalsIgnoreCase(status)) {
            return "disabled";
        }
        return switch (normalizeChannel(channelType)) {
            case "in_app" -> featureProperties.isSiteMessageEnabled() ? "healthy" : "unhealthy";
            case "email" -> featureProperties.isEmailEnabled() ? "healthy" : "unhealthy";
            case "sms" -> featureProperties.isSmsEnabled() ? "healthy" : "unhealthy";
            case "feishu" -> "healthy";
            default -> "unknown";
        };
    }

    private void ensureChannelEnabled(String channel) {
        switch (normalizeChannel(channel)) {
            case "in_app" -> {
                if (!featureProperties.isSiteMessageEnabled()) {
                    throw new BusinessException(400, "in_app channel is disabled");
                }
            }
            case "email" -> {
                if (!featureProperties.isEmailEnabled()) {
                    throw new BusinessException(400, "email channel is disabled");
                }
            }
            case "sms" -> {
                if (!featureProperties.isSmsEnabled()) {
                    throw new BusinessException(400, "sms channel is disabled");
                }
            }
            default -> {
            }
        }
    }

    private void validateRecipient(String recipientType, String recipientValue) {
        if (!StringUtils.hasText(recipientValue)) {
            throw new BusinessException(400, "recipient value is required");
        }
        switch (recipientType) {
            case "user_id" -> {
            }
            case "email" -> {
                if (!recipientValue.contains("@")) {
                    throw new BusinessException(400, "Invalid email recipient");
                }
            }
            case "phone" -> {
                if (!recipientValue.matches("^1\\d{10}$")) {
                    throw new BusinessException(400, "Invalid phone recipient");
                }
            }
            case "feishu_open_id", "feishu_user_id", "feishu_union_id" -> {
            }
            default -> throw new BusinessException(400, "Unsupported recipient type: " + recipientType);
        }
    }

    private TemplateDtos.Record mapTemplateRecord(ResultSet rs, int rowNum) throws SQLException {
        TemplateDtos.Record record = new TemplateDtos.Record();
        record.setTemplateId(rs.getString("id"));
        record.setTemplateCode(rs.getString("template_code"));
        record.setBusinessCode(rs.getString("business_code"));
        record.setName(rs.getString("name"));
        record.setChannel(rs.getString("channel"));
        record.setContent(parseJsonNode(rs.getString("content")));
        record.setParamsSchema(parseJsonNode(rs.getString("params_schema")));
        record.setVersion(rs.getInt("version"));
        record.setStatus(rs.getString("status"));
        record.setCreatedBy(rs.getString("created_by"));
        record.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_time")));
        record.setUpdatedAt(toLocalDateTime(rs.getTimestamp("update_time")));
        return record;
    }

    private TemplateDtos.VersionRecord mapTemplateVersion(ResultSet rs, int rowNum) throws SQLException {
        TemplateDtos.VersionRecord record = new TemplateDtos.VersionRecord();
        record.setTemplateId(rs.getString("template_id"));
        record.setVersion(rs.getInt("version"));
        record.setTemplateCode(rs.getString("template_code"));
        record.setBusinessCode(rs.getString("business_code"));
        record.setName(rs.getString("name"));
        record.setChannel(rs.getString("channel"));
        record.setContent(parseJsonNode(rs.getString("content")));
        record.setParamsSchema(parseJsonNode(rs.getString("params_schema")));
        record.setStatus(rs.getString("status"));
        record.setChangeReason(rs.getString("change_reason"));
        record.setCreatedBy(rs.getString("created_by"));
        record.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_time")));
        return record;
    }

    private MessageDtos.MessageRecord mapMessageRecord(ResultSet rs, int rowNum) throws SQLException {
        MessageDtos.MessageRecord record = new MessageDtos.MessageRecord();
        record.setMessageId(rs.getString("id"));
        record.setBatchId(rs.getString("batch_id"));
        record.setRequestId(rs.getString("request_id"));
        record.setPolicyId(rs.getString("policy_id"));
        record.setBusinessCode(rs.getString("business_code"));
        record.setTemplateId(rs.getString("template_id"));
        record.setTemplateCode(rs.getString("template_code"));
        record.setChannel(rs.getString("channel_type"));
        record.setProvider(rs.getString("provider"));
        record.setRecipientType(rs.getString("recipient_type"));
        record.setRecipient(rs.getString("recipient_value"));
        record.setStatus(rs.getString("status"));
        record.setRetryCount(rs.getInt("retry_count"));
        record.setMaxRetry(rs.getInt("max_retry"));
        record.setTitle(rs.getString("content_title"));
        record.setContent(rs.getString("content_body"));
        record.setParams(parseJsonNode(rs.getString("params_json")));
        record.setTemplateSnapshot(parseJsonNode(rs.getString("template_snapshot")));
        record.setResponse(rs.getString("response"));
        record.setFailureReason(rs.getString("failure_reason"));
        record.setPlannedAt(toLocalDateTime(rs.getTimestamp("send_time_planned")));
        record.setSentAt(toLocalDateTime(rs.getTimestamp("send_time_actual")));
        record.setNextRetryAt(toLocalDateTime(rs.getTimestamp("next_retry_time")));
        record.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_time")));
        record.setUpdatedAt(toLocalDateTime(rs.getTimestamp("update_time")));
        return record;
    }

    private PolicyDtos.Record mapPolicyRecord(ResultSet rs, int rowNum) throws SQLException {
        PolicyDtos.Record record = new PolicyDtos.Record();
        record.setPolicyId(rs.getString("id"));
        record.setPolicyName(rs.getString("policy_name"));
        record.setTemplateId(rs.getString("template_id"));
        record.setTemplateCode(rs.getString("template_code"));
        record.setCarrierId(rs.getString("carrier_id"));
        record.setChannelType(rs.getString("channel_type"));
        record.setBusinessCode(rs.getString("business_code"));
        record.setDynamicCondition(rs.getString("dynamic_condition"));
        record.setSendMode(rs.getString("send_mode"));
        record.setSendTime(toLocalDateTime(rs.getTimestamp("send_time")));
        record.setRecurringRule(rs.getString("recurring_rule"));
        record.setRecurringEndTime(toLocalDateTime(rs.getTimestamp("recurring_end_time")));
        record.setNextFireTime(toLocalDateTime(rs.getTimestamp("next_fire_time")));
        record.setLastFireTime(toLocalDateTime(rs.getTimestamp("last_fire_time")));
        record.setPriority(rs.getInt("priority"));
        record.setMaxRetry(rs.getInt("max_retry"));
        record.setRemark(rs.getString("remark"));
        record.setStatus(rs.getString("status"));
        record.setCreateTime(toLocalDateTime(rs.getTimestamp("create_time")));
        record.setUpdateTime(toLocalDateTime(rs.getTimestamp("update_time")));
        return record;
    }

    private MessageRow mapMessageRow(ResultSet rs, int rowNum) throws SQLException {
        return new MessageRow(
            rs.getString("id"),
            rs.getString("task_id"),
            rs.getString("carrier_id"),
            rs.getString("channel_type"),
            rs.getString("provider"),
            rs.getString("business_code"),
            rs.getString("recipient_type"),
            rs.getString("recipient_value"),
            parseJsonNode(rs.getString("params_json")),
            rs.getString("content_title"),
            rs.getString("content_body"),
            parseJsonNode(rs.getString("carrier_config_json")),
            rs.getString("status"),
            rs.getInt("retry_count"),
            rs.getInt("max_retry"),
            rs.getInt("canceled") == 1
        );
    }

    private TemplateDtos.VersionRecord toTemplateVersionRecord(TemplateDtos.Record template) {
        TemplateDtos.VersionRecord record = new TemplateDtos.VersionRecord();
        record.setTemplateId(template.getTemplateId());
        record.setVersion(template.getVersion());
        record.setTemplateCode(template.getTemplateCode());
        record.setBusinessCode(template.getBusinessCode());
        record.setName(template.getName());
        record.setChannel(template.getChannel());
        record.setContent(template.getContent());
        record.setParamsSchema(template.getParamsSchema());
        record.setStatus(template.getStatus());
        record.setChangeReason(null);
        record.setCreatedBy(template.getCreatedBy());
        record.setCreatedAt(template.getCreatedAt());
        return record;
    }

    private JsonNode parseJsonNode(String value) {
        if (!StringUtils.hasText(value)) {
            return NullNode.getInstance();
        }
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(500, "Invalid JSON data stored");
        }
    }

    private String toJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node == null ? NullNode.getInstance() : node);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(500, "Failed to serialize JSON");
        }
    }

    private String toJsonOrNull(JsonNode node) {
        return node == null || node.isNull() ? "null" : toJson(node);
    }

    private LocalDateTime parseScheduleAt(String rawScheduleAt) {
        if (!StringUtils.hasText(rawScheduleAt)) {
            return LocalDateTime.now();
        }
        return parseDateTime(rawScheduleAt.trim());
    }

    private LocalDateTime parseDateTime(String rawValue) {
        try {
            return LocalDateTime.parse(rawValue);
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(rawValue, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(rawValue).atStartOfDay();
        } catch (Exception ignored) {
        }
        throw new BusinessException(400, "Invalid datetime: " + rawValue);
    }

    private LocalDateTime parseDateBoundary(String rawValue, boolean start) {
        try {
            return parseDateTime(rawValue);
        } catch (BusinessException ignored) {
        }
        LocalDate date = LocalDate.parse(rawValue);
        return start ? date.atStartOfDay() : date.atTime(23, 59, 59);
    }

    private String normalizeChannel(String channel) {
        if (!StringUtils.hasText(channel)) {
            throw new BusinessException(400, "channel is required");
        }
        return switch (channel.trim().toLowerCase(Locale.ROOT)) {
            case "site_message", "site-message", "in_app", "in-app", "inapp" -> "in_app";
            case "email" -> "email";
            case "sms" -> "sms";
            case "wechat", "wecom", "wechat_work" -> "wechat";
            case "feishu", "lark" -> "feishu";
            default -> throw new BusinessException(400, "Unsupported channel: " + channel);
        };
    }

    private String normalizeRecipientType(String recipientType) {
        if (!StringUtils.hasText(recipientType)) {
            throw new BusinessException(400, "recipient type is required");
        }
        return switch (recipientType.trim().toLowerCase(Locale.ROOT)) {
            case "user_id", "user-id", "uid" -> "user_id";
            case "email" -> "email";
            case "phone", "mobile", "sms" -> "phone";
            case "feishu_open_id", "feishu-open-id", "open_id", "open-id" -> "feishu_open_id";
            case "feishu_user_id", "feishu-user-id", "feishu_uid", "feishu-uid" -> "feishu_user_id";
            case "feishu_union_id", "feishu-union-id", "union_id", "union-id" -> "feishu_union_id";
            default -> throw new BusinessException(400, "Unsupported recipient type: " + recipientType);
        };
    }

    private String normalizeTemplateStatus(String status, String fallback) {
        if (!StringUtils.hasText(status)) {
            return fallback;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        if (!List.of("draft", "active", "archived").contains(normalized)) {
            throw new BusinessException(400, "Unsupported template status: " + status);
        }
        return normalized;
    }

    private String normalizePolicyStatus(String status, String fallback) {
        if (!StringUtils.hasText(status)) {
            return fallback;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        if (!List.of("active", "disabled", "draft").contains(normalized)) {
            throw new BusinessException(400, "Unsupported policy status: " + status);
        }
        return normalized;
    }

    private String normalizeSendMode(String sendMode) {
        String normalized = sendMode.trim().toLowerCase(Locale.ROOT);
        if (!List.of("immediate", "scheduled_once", "scheduled_recurring").contains(normalized)) {
            throw new BusinessException(400, "Unsupported send mode: " + sendMode);
        }
        return normalized;
    }

    private String normalizeChannelStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "active";
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        if (!List.of("active", "disabled").contains(normalized)) {
            throw new BusinessException(400, "Unsupported channel status: " + status);
        }
        return normalized;
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizeSize(Integer size) {
        int normalized = size == null || size < 1 ? 20 : size;
        return Math.min(normalized, 200);
    }

    private int defaultInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private void validateDynamicCondition(String dynamicCondition) {
        try {
            objectMapper.readTree(dynamicCondition);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(400, "dynamicCondition must be valid JSON");
        }
    }

    private void validatePolicySchedule(String sendMode, String sendTime, String recurringRule, String recurringEndTime) {
        switch (sendMode) {
            case "scheduled_once" -> {
                if (!StringUtils.hasText(sendTime)) {
                    throw new BusinessException(400, "sendTime is required for scheduled_once");
                }
            }
            case "scheduled_recurring" -> {
                if (!StringUtils.hasText(recurringRule)) {
                    throw new BusinessException(400, "recurringRule is required for scheduled_recurring");
                }
                CronExpression.parse(recurringRule.trim());
                if (StringUtils.hasText(recurringEndTime)) {
                    parseDateTime(recurringEndTime.trim());
                }
            }
            case "immediate" -> {
            }
            default -> throw new BusinessException(400, "Unsupported send mode: " + sendMode);
        }
    }

    private String defaultActor(String actor) {
        return StringUtils.hasText(actor) ? actor.trim() : "system";
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String shortId() {
        return generateId().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Timestamp toTimestampOrNull(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        return Timestamp.valueOf(parseDateTime(rawValue.trim()));
    }

    private String resolveNextFireTime(String sendMode, String sendTime, String explicitNextFireTime, String recurringRule, String recurringEndTime) {
        if (StringUtils.hasText(explicitNextFireTime)) {
            return capRecurringNextFireTime(sendMode, explicitNextFireTime.trim(), recurringEndTime);
        }
        String nextFireTime = switch (sendMode) {
            case "immediate" -> null;
            case "scheduled_once" -> sendTime;
            case "scheduled_recurring" -> {
                LocalDateTime baseTime = StringUtils.hasText(sendTime) ? parseDateTime(sendTime.trim()) : LocalDateTime.now();
                CronExpression cron = CronExpression.parse(recurringRule.trim());
                var next = cron.next(baseTime.atZone(ZoneId.systemDefault()));
                yield next == null ? null : next.toLocalDateTime().toString();
            }
            default -> null;
        };
        return capRecurringNextFireTime(sendMode, nextFireTime, recurringEndTime);
    }

    private String resolvePolicyNextFireTimeAfterFire(PolicyDtos.Record policy) {
        return switch (policy.getSendMode()) {
            case "scheduled_recurring" -> {
                if (!StringUtils.hasText(policy.getRecurringRule())) {
                    yield null;
                }
                CronExpression cron = CronExpression.parse(policy.getRecurringRule().trim());
                LocalDateTime base = LocalDateTime.now();
                var next = cron.next(base.atZone(ZoneId.systemDefault()));
                if (next == null) {
                    yield null;
                }
                LocalDateTime nextTime = next.toLocalDateTime();
                LocalDateTime endTime = policy.getRecurringEndTime();
                yield endTime != null && nextTime.isAfter(endTime) ? null : nextTime.toString();
            }
            default -> null;
        };
    }

    private String capRecurringNextFireTime(String sendMode, String nextFireTime, String recurringEndTime) {
        if (!"scheduled_recurring".equals(sendMode) || !StringUtils.hasText(nextFireTime)) {
            return nextFireTime;
        }
        if (!StringUtils.hasText(recurringEndTime)) {
            return nextFireTime;
        }
        LocalDateTime next = parseDateTime(nextFireTime.trim());
        LocalDateTime end = parseDateTime(recurringEndTime.trim());
        return next.isAfter(end) ? null : nextFireTime;
    }

    private String shorten(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String extractStringParam(JsonNode params, String key) {
        if (params == null || params.isNull() || !params.hasNonNull(key)) {
            return null;
        }
        String value = params.get(key).asText();
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String buildErrorResponse(String message) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", "error");
        response.put("message", shorten(message, 500));
        return response.toString();
    }

    private Map<String, Object> jsonNodeToContextMap(JsonNode node) {
        if (node == null || node.isNull()) {
            return new HashMap<>();
        }
        return objectMapper.convertValue(node, objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));
    }

    private <T> T querySingle(String sql, RowMapper<T> rowMapper, Object... args) {
        List<T> rows = jdbcTemplate.query(sql, rowMapper, args);
        if (rows.isEmpty()) {
            throw new BusinessException(404, "Resource not found");
        }
        return rows.get(0);
    }

    private record TemplateVersionSource(
        String templateId,
        String templateCode,
        String businessCode,
        String channel,
        JsonNode content,
        JsonNode paramsSchema
    ) {
    }

    private record ChannelCarrier(
        String id,
        String provider,
        int maxRetry
    ) {
    }

    private record FeishuTenantToken(
        String token,
        long expiresAtMillis
    ) {
    }

    private record FeishuIdentity(
        String feishuOpenId,
        String feishuUserId,
        String feishuUnionId,
        String enterpriseEmail
    ) {
    }

    private static class MessageRequestContext {
        private String businessCode;
        private String templateId;
        private String templateCode;
        private String channel;
        private String carrierId;
        private String provider;
        private int maxRetry;
        private int priority;
        private JsonNode params;
        private RenderedContent renderedContent;
    }

    private record MessageRow(
        String id,
        String taskId,
        String carrierId,
        String channelType,
        String provider,
        String businessCode,
        String recipientType,
        String recipientValue,
        JsonNode params,
        String contentTitle,
        String contentBody,
        JsonNode carrierConfig,
        String status,
        int retryCount,
        int maxRetry,
        boolean canceled
    ) {
    }
}
