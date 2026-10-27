package com.example.usermgmt.notification.config;

import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initialize() {
        List<String> ddlStatements = List.of(
            """
            CREATE TABLE IF NOT EXISTS `carrier` (
              `id` VARCHAR(64) NOT NULL,
              `channel_name` VARCHAR(50) NOT NULL,
              `channel_type` VARCHAR(20) NOT NULL,
              `channel_provider` VARCHAR(30) NOT NULL,
              `config_json` JSON NOT NULL,
              `priority` TINYINT NOT NULL DEFAULT 1,
              `weight` INT NOT NULL DEFAULT 100,
              `max_retry` TINYINT NOT NULL DEFAULT 3,
              `remark` VARCHAR(255) DEFAULT NULL,
              `status` VARCHAR(32) NOT NULL DEFAULT 'active',
              `is_default` TINYINT UNSIGNED NOT NULL DEFAULT 0,
              `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (`id`),
              UNIQUE KEY `uk_carrier_channel_provider` (`channel_type`, `channel_provider`),
              KEY `idx_carrier_status` (`channel_type`, `status`, `is_default`, `priority`),
              KEY `idx_carrier_default` (`channel_type`, `is_default`, `status`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='渠道载体表'
            """,
            """
            CREATE TABLE IF NOT EXISTS `template` (
              `id` VARCHAR(64) NOT NULL,
              `template_code` VARCHAR(50) NOT NULL,
              `template_name` VARCHAR(50) NOT NULL,
              `carrier_id` VARCHAR(64) DEFAULT NULL,
              `channel_code` VARCHAR(20) NOT NULL,
              `provider_template_id` VARCHAR(100) DEFAULT NULL,
              `business_code` VARCHAR(50) NOT NULL,
              `content` JSON NOT NULL,
              `variable_definition` JSON DEFAULT NULL,
              `version` VARCHAR(32) NOT NULL DEFAULT '1',
              `remark` VARCHAR(255) DEFAULT NULL,
              `status` VARCHAR(32) NOT NULL DEFAULT 'draft',
              `created_by` VARCHAR(64) DEFAULT NULL,
              `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (`id`),
              UNIQUE KEY `uk_template_code` (`template_code`),
              KEY `idx_template_business` (`business_code`, `channel_code`, `status`),
              KEY `idx_template_carrier` (`carrier_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板表'
            """,
            """
            CREATE TABLE IF NOT EXISTS `task` (
              `id` VARCHAR(64) NOT NULL,
              `policy_id` VARCHAR(64) DEFAULT NULL,
              `batch_id` VARCHAR(64) DEFAULT NULL,
              `template_id` VARCHAR(64) NOT NULL,
              `template_code` VARCHAR(50) NOT NULL,
              `carrier_id` VARCHAR(64) NOT NULL,
              `channel_code` VARCHAR(20) NOT NULL,
              `business_type` VARCHAR(50) NOT NULL,
              `sender_uid` VARCHAR(64) NOT NULL DEFAULT 'system',
              `url` VARCHAR(100) DEFAULT NULL,
              `receiverlist` JSON NOT NULL,
              `params` JSON DEFAULT NULL,
              `template_snapshot` TEXT,
              `send_time_planned` DATETIME NOT NULL,
              `max_retry` TINYINT DEFAULT 3,
              `remark` VARCHAR(255) DEFAULT NULL,
              `status` VARCHAR(32) NOT NULL DEFAULT 'waiting',
              `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (`id`),
              KEY `idx_task_policy` (`policy_id`),
              KEY `idx_task_batch` (`batch_id`),
              KEY `idx_task_status_time` (`status`, `send_time_planned`),
              KEY `idx_task_business` (`business_type`, `channel_code`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务表'
            """,
            """
            CREATE TABLE IF NOT EXISTS `policy` (
              `id` VARCHAR(64) NOT NULL,
              `policy_name` VARCHAR(100) NOT NULL,
              `business_code` VARCHAR(50) NOT NULL,
              `task_id` VARCHAR(64) DEFAULT NULL,
              `template_id` VARCHAR(64) DEFAULT NULL,
              `template_code` VARCHAR(50) DEFAULT NULL,
              `carrier_id` VARCHAR(64) DEFAULT NULL,
              `channel_type` VARCHAR(20) DEFAULT NULL,
              `dynamic_condition` TEXT,
              `send_mode` VARCHAR(20) NOT NULL,
              `send_time` DATETIME DEFAULT NULL,
              `recurring_rule` VARCHAR(100) DEFAULT NULL,
              `start_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              `end_time` DATETIME DEFAULT NULL,
              `recurring_end_time` DATETIME DEFAULT NULL,
              `max_executions` INT DEFAULT NULL,
              `last_execute_time` DATETIME DEFAULT NULL,
              `next_execute_time` DATETIME DEFAULT NULL,
              `last_fire_time` DATETIME DEFAULT NULL,
              `next_fire_time` DATETIME DEFAULT NULL,
              `priority` TINYINT DEFAULT 5,
              `max_retry` TINYINT DEFAULT 3,
              `remark` VARCHAR(255) DEFAULT NULL,
              `status` VARCHAR(32) NOT NULL DEFAULT 'active',
              `created_by` VARCHAR(64) DEFAULT NULL,
              `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (`id`),
              KEY `idx_policy_business` (`business_code`, `status`),
              KEY `idx_policy_task` (`task_id`),
              KEY `idx_policy_schedule` (`status`, `next_execute_time`, `next_fire_time`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='策略表'
            """,
            """
            CREATE TABLE IF NOT EXISTS `message` (
              `id` VARCHAR(64) NOT NULL,
              `task_id` VARCHAR(64) DEFAULT NULL,
              `batch_id` VARCHAR(64) DEFAULT NULL,
              `request_id` VARCHAR(64) DEFAULT NULL,
              `policy_id` VARCHAR(64) DEFAULT NULL,
              `template_id` VARCHAR(64) NOT NULL,
              `template_code` VARCHAR(50) NOT NULL,
              `carrier_id` VARCHAR(64) NOT NULL,
              `channel_code` VARCHAR(20) NOT NULL,
              `provider` VARCHAR(30) DEFAULT NULL,
              `business_type` VARCHAR(50) NOT NULL,
              `sender_uid` VARCHAR(64) NOT NULL DEFAULT 'system',
              `sender_contact` VARCHAR(100) DEFAULT NULL,
              `receiver_uid` VARCHAR(64) NOT NULL DEFAULT '',
              `receiver_contact` VARCHAR(100) NOT NULL,
              `recipient_type` VARCHAR(32) NOT NULL,
              `recipient_value` VARCHAR(255) NOT NULL,
              `params` JSON DEFAULT NULL,
              `content_title` VARCHAR(255) DEFAULT NULL,
              `content_body` TEXT NOT NULL,
              `template_snapshot` TEXT,
              `send_time_planned` DATETIME NOT NULL,
              `send_time` DATETIME DEFAULT NULL,
              `read_time` DATETIME DEFAULT NULL,
              `response` TEXT,
              `failure_reason` VARCHAR(500) DEFAULT NULL,
              `retry_count` TINYINT DEFAULT 0,
              `max_retry` TINYINT DEFAULT 3,
              `next_retry_time` DATETIME DEFAULT NULL,
              `remark` VARCHAR(255) DEFAULT NULL,
              `status` VARCHAR(32) NOT NULL DEFAULT 'pending',
              `created_by` VARCHAR(64) DEFAULT NULL,
              `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              `canceled` TINYINT UNSIGNED NOT NULL DEFAULT 0,
              PRIMARY KEY (`id`),
              KEY `idx_message_task` (`task_id`),
              KEY `idx_message_batch` (`batch_id`),
              KEY `idx_message_request` (`request_id`),
              KEY `idx_message_status_time` (`status`, `send_time_planned`),
              KEY `idx_message_business` (`business_type`, `channel_code`),
              KEY `idx_message_recipient` (`recipient_type`, `recipient_value`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表'
            """,
            """
            CREATE TABLE IF NOT EXISTS `consumed_meeting_event` (
              `event_id` VARCHAR(64) NOT NULL,
              `event_type` VARCHAR(100) NOT NULL,
              `status` VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
              `attempts` INT NOT NULL DEFAULT 0,
              `last_error` VARCHAR(1000) DEFAULT NULL,
              `processed_time` DATETIME DEFAULT NULL,
              `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (`event_id`),
              KEY `idx_consumed_event_status` (`status`, `update_time`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议事件消费幂等表'
            """
        );

        ddlStatements.forEach(jdbcTemplate::execute);
        ensureCarrierDefaultMetadata();
        seedDefaults();
        seedDefaultEmailCarrierConfig();
        seedDefaultCarrierFlags();
    }

    private void ensureCarrierDefaultMetadata() {
        if (!columnExists("carrier", "is_default")) {
            jdbcTemplate.execute("ALTER TABLE `carrier` ADD COLUMN `is_default` TINYINT UNSIGNED NOT NULL DEFAULT 0 AFTER `status`");
        }
        ensureIndexColumns(
            "carrier",
            "idx_carrier_status",
            List.of("channel_type", "status", "is_default", "priority"),
            "CREATE INDEX `idx_carrier_status` ON `carrier` (`channel_type`, `status`, `is_default`, `priority`)"
        );
        if (!indexExists("carrier", "idx_carrier_default")) {
            jdbcTemplate.execute("CREATE INDEX `idx_carrier_default` ON `carrier` (`channel_type`, `is_default`, `status`)");
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
            """,
            Integer.class,
            tableName,
            columnName
        );
        return count != null && count > 0;
    }

    private void ensureIndexColumns(String tableName, String indexName, List<String> expectedColumns, String createSql) {
        if (indexColumnsMatch(tableName, indexName, expectedColumns)) {
            return;
        }
        if (indexExists(tableName, indexName)) {
            jdbcTemplate.execute("DROP INDEX `" + indexName + "` ON `" + tableName + "`");
        }
        jdbcTemplate.execute(createSql);
    }

    private boolean indexColumnsMatch(String tableName, String indexName, List<String> expectedColumns) {
        List<String> columns = jdbcTemplate.queryForList(
            """
            SELECT column_name
            FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?
            ORDER BY seq_in_index
            """,
            String.class,
            tableName,
            indexName
        );
        return columns.equals(expectedColumns);
    }

    private boolean indexExists(String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?
            """,
            Integer.class,
            tableName,
            indexName
        );
        return count != null && count > 0;
    }

    private void seedDefaults() {
        jdbcTemplate.update(
            """
            INSERT IGNORE INTO `carrier`
                (`id`, `channel_name`, `channel_type`, `channel_provider`, `config_json`, `priority`, `weight`, `max_retry`, `remark`, `status`, `is_default`)
            VALUES
                ('carrier-in-app-system', 'System In-App', 'in_app', 'system', JSON_OBJECT(), 1, 100, 3, 'Default in-app carrier', 'active', 1),
                ('carrier-email-spring', 'Spring Mail', 'email', 'spring_mail', JSON_OBJECT(), 1, 100, 3, 'Default spring mail carrier', 'active', 1),
                ('carrier-sms-spug', 'Spug SMS', 'sms', 'spug_sms', JSON_OBJECT(
                    'mode', 'template',
                    'baseUrl', 'https://push.spug.cc/send',
                    'templateId', '',
                    'targetParam', 'targets',
                    'timeoutMs', 5000
                ), 1, 100, 3, 'Spug sms carrier', 'disabled', 0)
            """
        );
        jdbcTemplate.update(
            """
            INSERT IGNORE INTO `template`
                (`id`, `template_code`, `template_name`, `carrier_id`, `channel_code`, `provider_template_id`,
                 `business_code`, `content`, `variable_definition`, `version`, `remark`, `status`, `created_by`)
            VALUES
                (
                    'tpl-in-app-plain-message',
                    'IN_APP_PLAIN_MESSAGE',
                    'Default In-App Plain Message',
                    'carrier-in-app-system',
                    'in_app',
                    NULL,
                    'system',
                    JSON_OBJECT('title', '{{ title | default:"System Message" }}', 'body', '{{ body }}'),
                    JSON_OBJECT('title', 'string', 'body', 'string', 'link', 'string'),
                    '1',
                    'Seeded default in-app template',
                    'active',
                    'system'
                ),
                (
                    'tpl-feishu-plain-message',
                    'FEISHU_PLAIN_MESSAGE',
                    'Default Feishu Plain Message',
                    NULL,
                    'feishu',
                    NULL,
                    'system',
                    JSON_OBJECT('title', '{{ title | default:"System Message" }}', 'body', '{{ body }}'),
                    JSON_OBJECT('title', 'string', 'body', 'string'),
                    '1',
                    'Seeded default Feishu text template',
                    'active',
                    'system'
                )
            """
        );
    }

    private void seedDefaultCarrierFlags() {
        markSeedCarrierAsDefaultIfMissing("in_app", "carrier-in-app-system");
        markSeedCarrierAsDefaultIfMissing("email", "carrier-email-spring");
    }

    private void seedDefaultEmailCarrierConfig() {
        jdbcTemplate.update(
            """
            UPDATE `carrier`
            SET config_json = JSON_OBJECT(
                    'host', 'smtp.163.com',
                    'port', 465,
                    'protocol', 'smtps',
                    'defaultEncoding', 'UTF-8',
                    'username', '',
                    'password', '',
                    'from', '',
                    'properties', JSON_OBJECT(
                        'mail', JSON_OBJECT(
                            'smtp', JSON_OBJECT(
                                'auth', true,
                                'ssl', JSON_OBJECT('enable', true),
                                'socketFactory', JSON_OBJECT('class', 'javax.net.ssl.SSLSocketFactory'),
                                'connectiontimeout', 5000,
                                'timeout', 5000,
                                'writetimeout', 5000
                            )
                        )
                    )
                ),
                update_time = NOW()
            WHERE id = 'carrier-email-spring' AND JSON_LENGTH(config_json) = 0
            """
        );
    }

    private void markSeedCarrierAsDefaultIfMissing(String channelType, String carrierId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM `carrier` WHERE channel_type = ? AND status = 'active' AND is_default = 1",
            Integer.class,
            channelType
        );
        if (count == null || count == 0) {
            jdbcTemplate.update("UPDATE `carrier` SET is_default = 1, update_time = NOW() WHERE id = ?", carrierId);
        }
    }
}
