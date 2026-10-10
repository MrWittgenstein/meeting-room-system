CREATE DATABASE IF NOT EXISTS meetingroom_users CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE meetingroom_users;

SET @legacy_column_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'user' AND column_name = 'legacy_user_id'
);
SET @legacy_column_sql := IF(
    @legacy_column_exists = 0,
    'ALTER TABLE `user` ADD COLUMN legacy_user_id INT NULL COMMENT ''meetingroom.user.user_id''',
    'SELECT 1'
);
PREPARE legacy_column_stmt FROM @legacy_column_sql;
EXECUTE legacy_column_stmt;
DEALLOCATE PREPARE legacy_column_stmt;

SET @legacy_cipher_column_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'user' AND column_name = 'legacy_email_cipher'
);
SET @legacy_cipher_column_sql := IF(
    @legacy_cipher_column_exists = 0,
    'ALTER TABLE `user` ADD COLUMN legacy_email_cipher VARCHAR(255) NULL COMMENT ''encrypted meetingroom email''',
    'SELECT 1'
);
PREPARE legacy_cipher_column_stmt FROM @legacy_cipher_column_sql;
EXECUTE legacy_cipher_column_stmt;
DEALLOCATE PREPARE legacy_cipher_column_stmt;

SET @legacy_hash_column_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'user' AND column_name = 'legacy_email_hash'
);
SET @legacy_hash_column_sql := IF(
    @legacy_hash_column_exists = 0,
    'ALTER TABLE `user` ADD COLUMN legacy_email_hash VARCHAR(64) NULL COMMENT ''meetingroom email login hash''',
    'SELECT 1'
);
PREPARE legacy_hash_column_stmt FROM @legacy_hash_column_sql;
EXECUTE legacy_hash_column_stmt;
DEALLOCATE PREPARE legacy_hash_column_stmt;

SET @legacy_index_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'user' AND index_name = 'uk_user_legacy_user_id'
);
SET @legacy_index_sql := IF(
    @legacy_index_exists = 0,
    'CREATE UNIQUE INDEX uk_user_legacy_user_id ON `user` (legacy_user_id)',
    'SELECT 1'
);
PREPARE legacy_index_stmt FROM @legacy_index_sql;
EXECUTE legacy_index_stmt;
DEALLOCATE PREPARE legacy_index_stmt;

UPDATE meetingroom_users.`user` u
JOIN meetingroom.`user` m ON u.phone = NULLIF(m.phone, '')
SET u.legacy_user_id = m.user_id
WHERE u.legacy_user_id IS NULL
  AND m.phone <> ''
  AND (SELECT COUNT(*) FROM meetingroom.`user` same_phone WHERE same_phone.phone = m.phone) = 1;

UPDATE meetingroom_users.`user` u
JOIN meetingroom.`user` m ON u.legacy_user_id = m.user_id
SET u.legacy_email_cipher = m.email
WHERE m.email IS NOT NULL AND m.email <> '';

UPDATE meetingroom_users.`user` u
JOIN meetingroom.`user` m ON u.legacy_user_id = m.user_id
SET u.legacy_email_hash = m.email_hash
WHERE m.email_hash IS NOT NULL AND m.email_hash <> '';

INSERT INTO `user` (
    id, name, phone, mail, password, salt, gender, description, extension,
    status, creator_id, create_time, last_update_time, last_operator_id,
    extra_data, legacy_user_id, legacy_email_cipher, legacy_email_hash
)
SELECT
    REPLACE(UUID(), '-', ''),
    m.username,
    CASE
        WHEN m.phone <> ''
         AND (SELECT COUNT(*) FROM meetingroom.`user` same_phone WHERE same_phone.phone = m.phone) = 1
         AND NOT EXISTS (SELECT 1 FROM meetingroom_users.`user` same_phone_user WHERE same_phone_user.phone = m.phone)
        THEN m.phone
        ELSE NULL
    END,
    NULL,
    m.password,
    NULL,
    0,
    m.real_name,
    NULL,
    CASE WHEN m.status = 0 THEN 1 ELSE 2 END,
    'meetingroom-migration',
    COALESCE(m.create_time, CURRENT_TIMESTAMP),
    COALESCE(m.update_time, CURRENT_TIMESTAMP),
    'meetingroom-migration',
    JSON_OBJECT(
        'role', CASE m.user_type WHEN 0 THEN 'room_admin' WHEN 2 THEN 'super_admin' ELSE 'user' END,
        'avatar', m.thumbnailurl,
        'legacyUsername', m.username
    ),
    m.user_id,
    m.email,
    m.email_hash
FROM meetingroom.`user` m
WHERE NOT EXISTS (
    SELECT 1 FROM meetingroom_users.`user` u WHERE u.legacy_user_id = m.user_id
);

INSERT INTO meetingroom_users.`group` (
    id, name, parent_id, type, status, description, creator_id, extra_data
)
VALUES (
    'group-admin', 'Meeting Room Admins', NULL, 'role_group', 1,
    'Migrated meeting-room administrators', 'meetingroom-migration',
    JSON_OBJECT('role', 'admin', 'builtIn', TRUE)
)
ON DUPLICATE KEY UPDATE status = 1;

INSERT IGNORE INTO meetingroom_users.user_and_group (
    user_id, group_id, creator_id, last_operator_id, description, type
)
SELECT
    target.id,
    CASE WHEN source.user_type IN (0, 2) THEN 'group-admin' ELSE 'group-normal-user' END,
    'meetingroom-migration',
    'meetingroom-migration',
    'Migrated from meetingroom.user',
    CASE WHEN source.user_type IN (0, 2) THEN 'admin' ELSE 'member' END
FROM meetingroom.`user` source
JOIN meetingroom_users.`user` target ON target.legacy_user_id = source.user_id;

INSERT IGNORE INTO meetingroom_users.permission_and_group (
    group_id, permission_id, creator_id, description
)
SELECT 'group-admin', id, 'meetingroom-migration', 'Migrated admin group permissions'
FROM meetingroom_users.permission
WHERE status = 1;
