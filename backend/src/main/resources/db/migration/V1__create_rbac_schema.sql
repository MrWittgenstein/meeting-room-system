CREATE TABLE IF NOT EXISTS sys_role (
    role_id BIGINT NOT NULL AUTO_INCREMENT,
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id),
    UNIQUE KEY uk_sys_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_permission (
    permission_id BIGINT NOT NULL AUTO_INCREMENT,
    permission_code VARCHAR(100) NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (permission_id),
    UNIQUE KEY uk_sys_permission_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_role (
    user_id INT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    KEY idx_user_role_role (role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES `user` (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (role_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id),
    KEY idx_role_permission_permission (permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role (role_id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission (permission_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_role (role_code, role_name) VALUES
    ('user', '普通用户'),
    ('room_admin', '会议室管理员'),
    ('super_admin', '超级管理员')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), enabled = 1;

INSERT INTO sys_permission (permission_code, permission_name) VALUES
    ('room:read', '查看会议室'),
    ('room:manage', '管理会议室'),
    ('reservation:create', '创建预约'),
    ('reservation:read:self', '查看自己的预约'),
    ('reservation:cancel:self', '取消自己的预约'),
    ('reservation:approve', '审批预约'),
    ('user:read:self', '查看个人信息'),
    ('user:update:self', '修改个人信息'),
    ('user:manage', '管理用户'),
    ('news:read', '查看重要事项'),
    ('news:manage', '管理重要事项'),
    ('statistics:read', '查看统计数据'),
    ('iot:read', '查看物联网数据'),
    ('iot:control', '控制物联网设备')
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), enabled = 1;

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'room:read', 'reservation:create', 'reservation:read:self',
    'reservation:cancel:self', 'user:read:self', 'user:update:self', 'iot:read'
)
WHERE r.role_code = 'user';

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'room:read', 'room:manage', 'reservation:read:self', 'reservation:approve',
    'user:read:self', 'user:update:self', 'user:manage', 'news:read', 'news:manage',
    'statistics:read', 'iot:read', 'iot:control'
)
WHERE r.role_code = 'room_admin';

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM sys_role r
JOIN sys_permission p ON p.enabled = 1
WHERE r.role_code = 'super_admin';

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM `user` u
JOIN sys_role r ON r.role_code = CASE u.user_type
    WHEN 0 THEN 'room_admin'
    WHEN 2 THEN 'super_admin'
    ELSE 'user'
END;
