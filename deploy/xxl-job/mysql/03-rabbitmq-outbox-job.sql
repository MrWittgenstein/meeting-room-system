USE `xxl_job`;

INSERT INTO `xxl_job_info`
(`id`, `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
 `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
 `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`,
 `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
 `child_jobid`)
SELECT 110, `group`.`id`, '会议事件 Outbox 投递', NOW(), NOW(), 'meeting-room', '',
       'CRON', '0/5 * * * * ?', 'DO_NOTHING', 'FIRST', 'meetingEventOutboxDispatch', '',
       'SERIAL_EXECUTION', 0, 1, 'BEAN', '', '增量初始化', NOW(), ''
FROM `xxl_job_group` AS `group`
WHERE `group`.`app_name` = 'meetingroom-executor'
  AND NOT EXISTS (
      SELECT 1 FROM `xxl_job_info`
      WHERE `job_group` = `group`.`id` AND `executor_handler` = 'meetingEventOutboxDispatch'
  );

COMMIT;
