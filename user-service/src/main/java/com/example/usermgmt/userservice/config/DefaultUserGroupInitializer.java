package com.example.usermgmt.userservice.config;

import com.example.usermgmt.common.enums.UserStatus;
import com.example.usermgmt.userservice.support.BuiltInGroupConstants;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DefaultUserGroupInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    @Transactional
    public void initialize() {
        ensureNormalUserGroup();
        ensureNormalUserPermissions();
        ensureLookupPermissions();
        ensureOperationsMenuPermissions();
        grantNormalUserPermissions();
        grantAllPermissionsToAdminGroup();
        assignExistingNormalUsers();
    }

    private void ensureNormalUserGroup() {
        jdbcTemplate.update("""
            INSERT INTO `group` (id, name, parent_id, type, status, description, creator_id, extension, extra_data)
            VALUES (?, ?, NULL, 'role_group', 1, ?, 'system', NULL, ?)
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                status = VALUES(status),
                description = VALUES(description),
                extra_data = VALUES(extra_data)
            """,
            BuiltInGroupConstants.NORMAL_USER_GROUP_ID,
            BuiltInGroupConstants.NORMAL_USER_GROUP_NAME,
            "\u7cfb\u7edf\u9ed8\u8ba4\u666e\u901a\u7528\u6237\u5206\u7ec4",
            "{\"role\":\"user\",\"builtIn\":true,\"defaultForRegister\":true}"
        );
    }

    private void ensureNormalUserPermissions() {
        ensurePermission("perm-api-message-list", "api:message:list", "站内消息查询", "/api/v1/messages", 410);
        ensurePermission("perm-api-message-read", "api:message:read", "站内消息已读", "/api/v1/messages/read", 411);
        ensurePermission("perm-api-message-send", "api:message:send", "站内消息发送", "/api/v1/messages/send", 412);
        ensurePermission("perm-api-message-template-list", "api:message:template:list", "站内模板查询", "/api/v1/messages/templates", 413);
        ensurePermission("perm-api-message-template-preview", "api:message:template:preview", "站内模板预览", "/api/v1/messages/templates/preview", 414);
    }

    private void ensureLookupPermissions() {
        ensurePermission("perm-api-user-lookup", "api:user:lookup", "Message recipient lookup", "/api/v1/users/lookup", 415);
    }

    private void ensurePermission(String id, String code, String name, String path, int sort) {
        jdbcTemplate.update("""
            INSERT INTO permission (id, code, name, parent_id, path, icon, sort, status, creator_id, description, method_type)
            VALUES (?, ?, ?, NULL, ?, 'api', ?, 1, 'system', ?, 'api')
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                path = VALUES(path),
                sort = VALUES(sort),
                status = VALUES(status),
                last_update_time = CURRENT_TIMESTAMP
            """,
            id,
            code,
            name,
            path,
            sort,
            name
        );
    }

    private void grantNormalUserPermissions() {
        String placeholders = String.join(",", BuiltInGroupConstants.NORMAL_USER_PERMISSION_CODES.stream().map(item -> "?").toList());
        Object[] revokeArgs = new Object[BuiltInGroupConstants.NORMAL_USER_PERMISSION_CODES.size() + 1];
        revokeArgs[0] = BuiltInGroupConstants.NORMAL_USER_GROUP_ID;
        for (int i = 0; i < BuiltInGroupConstants.NORMAL_USER_PERMISSION_CODES.size(); i++) {
            revokeArgs[i + 1] = BuiltInGroupConstants.NORMAL_USER_PERMISSION_CODES.get(i);
        }
        jdbcTemplate.update("""
            DELETE pg
            FROM permission_and_group pg
            INNER JOIN permission p ON p.id = pg.permission_id
            WHERE pg.group_id = ?
              AND p.code NOT IN (%s)
            """.formatted(placeholders), revokeArgs);

        Object[] args = new Object[BuiltInGroupConstants.NORMAL_USER_PERMISSION_CODES.size() + 1];
        args[0] = BuiltInGroupConstants.NORMAL_USER_GROUP_ID;
        for (int i = 0; i < BuiltInGroupConstants.NORMAL_USER_PERMISSION_CODES.size(); i++) {
            args[i + 1] = BuiltInGroupConstants.NORMAL_USER_PERMISSION_CODES.get(i);
        }
        jdbcTemplate.update("""
            INSERT IGNORE INTO permission_and_group (group_id, permission_id, creator_id, description)
            SELECT ?, p.id, 'system', 'Default normal user permission'
            FROM permission p
            WHERE p.code IN (%s)
            """.formatted(placeholders), args);
    }

    private void ensureOperationsMenuPermissions() {
        ensureMenuPermission(
            "perm-device-sessions",
            "menu:device:sessions",
            "Device sessions",
            "/devices",
            "devices",
            520,
            "View and revoke current account sessions"
        );
        ensureMenuPermission(
            "perm-menu-notification-management",
            "menu:notificationManagement",
            "通知管理",
            "/notifications",
            "notification",
            600,
            "通知渠道、模板、消息记录和发送策略运维入口"
        );
        ensureMenuPermission(
            "perm-menu-log-management",
            "menu:logManagement",
            "日志运维",
            "/logs",
            "logs",
            610,
            "访问日志查询、指标分析和告警规则运维入口"
        );
        ensureChildMenuPermission(
            "perm-log-query",
            "menu:log:query",
            "访问日志",
            "perm-menu-log-management",
            "/logs/query",
            "list",
            611,
            "查询访问日志"
        );
        ensureChildMenuPermission(
            "perm-log-metrics",
            "menu:log:metrics",
            "指标分析",
            "perm-menu-log-management",
            "/logs/metrics",
            "chart",
            612,
            "查看日志指标和趋势图"
        );
        ensureChildMenuPermission(
            "perm-log-alerts",
            "menu:log:alerts",
            "告警规则",
            "perm-menu-log-management",
            "/logs/alerts",
            "alert",
            613,
            "维护日志告警规则"
        );
        ensureChildMenuPermission(
            "perm-log-triggers",
            "menu:log:triggers",
            "触发记录",
            "perm-menu-log-management",
            "/logs/triggers",
            "history",
            614,
            "查看日志告警触发记录"
        );
    }

    private void ensureMenuPermission(String id, String code, String name, String path, String icon, int sort, String description) {
        ensureMenuPermission(id, code, name, null, path, icon, sort, description);
    }

    private void ensureChildMenuPermission(String id, String code, String name, String parentId, String path, String icon, int sort, String description) {
        ensureMenuPermission(id, code, name, parentId, path, icon, sort, description);
    }

    private void ensureMenuPermission(String id, String code, String name, String parentId, String path, String icon, int sort, String description) {
        jdbcTemplate.update("""
            INSERT INTO permission (id, code, name, parent_id, path, icon, sort, status, creator_id, description, method_type)
            VALUES (?, ?, ?, ?, ?, ?, ?, 1, 'system', ?, 'menu')
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                parent_id = VALUES(parent_id),
                path = VALUES(path),
                icon = VALUES(icon),
                sort = VALUES(sort),
                status = VALUES(status),
                description = VALUES(description),
                method_type = VALUES(method_type),
                last_update_time = CURRENT_TIMESTAMP
            """,
            id,
            code,
            name,
            parentId,
            path,
            icon,
            sort,
            description
        );
    }

    private void grantAllPermissionsToAdminGroup() {
        jdbcTemplate.update("""
            INSERT IGNORE INTO permission_and_group (group_id, permission_id, creator_id, description)
            SELECT ?, p.id, 'system', 'Admin group default all active permissions'
            FROM permission p
            WHERE p.status = 1
            """, BuiltInGroupConstants.ADMIN_GROUP_ID);
    }

    private void assignExistingNormalUsers() {
        jdbcTemplate.update("""
            INSERT IGNORE INTO user_and_group (user_id, group_id, creator_id, last_operator_id, description, type)
            SELECT u.id, ?, 'system', 'system', 'Default normal user group', 'member'
            FROM `user` u
            WHERE COALESCE(JSON_UNQUOTE(JSON_EXTRACT(u.extra_data, '$.role')), 'user') <> 'admin'
              AND u.status = ?
              AND NOT EXISTS (
                  SELECT 1
                  FROM user_and_group admin_rel
                  WHERE admin_rel.user_id = u.id
                    AND admin_rel.group_id = ?
              )
            """,
            BuiltInGroupConstants.NORMAL_USER_GROUP_ID,
            UserStatus.ACTIVE.getCode(),
            BuiltInGroupConstants.ADMIN_GROUP_ID
        );
    }
}
