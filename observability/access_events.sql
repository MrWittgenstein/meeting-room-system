CREATE DATABASE IF NOT EXISTS meetingroom_logs;
CREATE TABLE IF NOT EXISTS meetingroom_logs.access_events (
    timestamp DateTime64(3, 'UTC'),
    event_id String,
    microservice LowCardinality(String),
    api_endpoint String,
    http_method LowCardinality(String),
    status_code UInt16,
    business_code Int32,
    latency_ms UInt64,
    trace_id String,
    user_id String DEFAULT '',
    client_ip String DEFAULT '',
    user_agent String DEFAULT '',
    device_id String DEFAULT '',
    room_id String DEFAULT '',
    command_id String DEFAULT '',
    outcome String DEFAULT '',
    exception String DEFAULT ''
) ENGINE = ReplacingMergeTree
PARTITION BY toYYYYMM(timestamp)
ORDER BY (microservice, timestamp, event_id)
TTL toDateTime(timestamp) + INTERVAL 30 DAY;
