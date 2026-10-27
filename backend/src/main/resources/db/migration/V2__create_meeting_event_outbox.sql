CREATE TABLE IF NOT EXISTS meeting_event_outbox (
    event_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    routing_key VARCHAR(100) NOT NULL,
    payload JSON NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_time DATETIME NULL,
    last_error VARCHAR(1000) NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (event_id),
    KEY idx_meeting_event_outbox_dispatch (status, next_attempt_at, created_time),
    KEY idx_meeting_event_outbox_type (event_type, created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
