USE `xxl_job`;

-- Remove the demo executors/jobs shipped with the official schema.
DELETE FROM `xxl_job_info`;
DELETE FROM `xxl_job_group`;

INSERT INTO `xxl_job_group` (`id`, `app_name`, `title`, `address_type`, `address_list`, `update_time`)
VALUES
    (10, 'meetingroom-executor', '会议室服务', 0, NULL, NOW()),
    (11, 'meeting-user-executor', '用户服务', 0, NULL, NOW()),
    (12, 'meeting-notification-executor', '消息通告服务', 0, NULL, NOW());

INSERT INTO `xxl_job_info`
(`id`, `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
 `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
 `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`,
 `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
 `child_jobid`)
VALUES
    (101, 10, '预约过期检查', NOW(), NOW(), 'meeting-room', '', 'CRON', '0 0/10 * * * ?',
     'DO_NOTHING', 'FIRST', 'reservationExpiryCheck', '', 'SERIAL_EXECUTION', 0, 1, 'BEAN', '', '初始化', NOW(), ''),
    (102, 10, '过期预约清理', NOW(), NOW(), 'meeting-room', '', 'CRON', '0 0 * * * ?',
     'DO_NOTHING', 'FIRST', 'expiredReservationCleanup', '', 'SERIAL_EXECUTION', 0, 1, 'BEAN', '', '初始化', NOW(), ''),
    (103, 10, '重要信息过期状态更新', NOW(), NOW(), 'meeting-room', '', 'CRON', '0 * * * * ?',
     'DO_NOTHING', 'FIRST', 'importantNewsExpiryUpdate', '', 'SERIAL_EXECUTION', 0, 1, 'BEAN', '', '初始化', NOW(), ''),
    (104, 10, '预约状态更新', NOW(), NOW(), 'meeting-room', '', 'CRON', '0 * * * * ?',
     'DO_NOTHING', 'FIRST', 'reservationStateUpdate', '', 'SERIAL_EXECUTION', 0, 1, 'BEAN', '', '初始化', NOW(), ''),
    (105, 10, '预约提醒发送', NOW(), NOW(), 'meeting-room', '', 'CRON', '0 * * * * ?',
     'DO_NOTHING', 'FIRST', 'reservationReminderDispatch', '', 'SERIAL_EXECUTION', 0, 1, 'BEAN', '', '初始化', NOW(), ''),
    (106, 11, '临时成员清理', NOW(), NOW(), 'meeting-room', '', 'CRON', '0 0 * * * ?',
     'DO_NOTHING', 'FIRST', 'temporaryMemberCleanup', '', 'SERIAL_EXECUTION', 0, 1, 'BEAN', '', '初始化', NOW(), ''),
    (107, 12, '消息派发', NOW(), NOW(), 'meeting-room', '', 'CRON', '0/5 * * * * ?',
     'DO_NOTHING', 'FIRST', 'notificationMessageDispatch', '', 'SERIAL_EXECUTION', 0, 1, 'BEAN', '', '初始化', NOW(), ''),
    (108, 12, '通知策略派发', NOW(), NOW(), 'meeting-room', '', 'CRON', '0/5 * * * * ?',
     'DO_NOTHING', 'FIRST', 'notificationPolicyDispatch', '', 'SERIAL_EXECUTION', 0, 1, 'BEAN', '', '初始化', NOW(), ''),
    (109, 10, '会议事件 Outbox 投递', NOW(), NOW(), 'meeting-room', '', 'CRON', '0/5 * * * * ?',
     'DO_NOTHING', 'FIRST', 'meetingEventOutboxDispatch', '', 'SERIAL_EXECUTION', 0, 1, 'BEAN', '', '初始化', NOW(), '');

COMMIT;
