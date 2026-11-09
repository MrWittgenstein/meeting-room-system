CREATE TABLE `user` (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    real_name VARCHAR(64),
    password VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    email_hash CHAR(32) UNIQUE,
    encrypted_email VARCHAR(255),
    phone VARCHAR(32),
    user_type INT NOT NULL,
    status INT NOT NULL,
    integral INT NOT NULL DEFAULT 0,
    originalurl VARCHAR(500),
    thumbnailurl VARCHAR(500),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE meetingroom (
    room_id INT AUTO_INCREMENT PRIMARY KEY,
    room_number INT NOT NULL UNIQUE,
    room_name VARCHAR(100) NOT NULL,
    capacity INT NOT NULL,
    location VARCHAR(100) NOT NULL,
    status INT NOT NULL DEFAULT 0,
    open_time TIME NOT NULL,
    close_time TIME NOT NULL,
    type VARCHAR(40),
    origin_image VARCHAR(500),
    thumbnail_image VARCHAR(500),
    description VARCHAR(255),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reservations (
    reservation_id INT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(40),
    room_name VARCHAR(100),
    room_number INT,
    location VARCHAR(100),
    user_id INT NOT NULL,
    approver_id INT,
    room_id INT NOT NULL,
    reserve_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status INT NOT NULL DEFAULT 0,
    state VARCHAR(80),
    username VARCHAR(64),
    purpose VARCHAR(500),
    approve_time DATETIME,
    reject_reason VARCHAR(500),
    cancel_reason VARCHAR(500),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_reservations_room_date (room_id, reserve_date, status),
    INDEX idx_reservations_user (user_id)
);

CREATE TABLE importantnews (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    related_room_id INT,
    news_type VARCHAR(40),
    start_time DATETIME,
    end_time DATETIME,
    severity VARCHAR(40),
    status VARCHAR(40),
    publisher_id INT,
    publisher_name VARCHAR(64),
    origin_image VARCHAR(500),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- The demo intentionally shares a documented password across synthetic accounts.
SET @demo_password = '$2b$12$RoRkmqtsS2TEwA1/clLuC.e.SH1jVyAZSczhm/C0.GkphgUdx./oW';
SET SESSION block_encryption_mode = 'aes-256-ecb';
SET @demo_key = UNHEX(SHA2('meeting-room-local-demo', 256));
INSERT INTO `user` (username, real_name, password, email, email_hash, user_type, status)
SELECT username, real_name, @demo_password, TO_BASE64(AES_ENCRYPT(email, @demo_key)), MD5(email), user_type, 0
FROM (
    SELECT 'demo-user' AS username, '体验用户' AS real_name, 'user@example.test' AS email, 1 AS user_type
    UNION ALL SELECT 'demo-manager', '体验审批员', 'manager@example.test', 0
    UNION ALL SELECT 'demo-admin', '体验管理员', 'admin@example.test', 2
) accounts;

INSERT INTO meetingroom (room_number, room_name, capacity, location, open_time, close_time, type, description)
VALUES (101, '一号会议室', 12, '体验区一楼', '08:00:00', '22:00:00', '小型会议室', '合成体验数据'),
       (201, '二号会议室', 24, '体验区二楼', '08:00:00', '22:00:00', '中型会议室', '合成体验数据');
