package com.example.usermgmt.userservice.config;

import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class SeataUndoLogInitializer {

    private final DataSource dataSource;

    @PostConstruct
    public void createUndoLogTable() throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS `undo_log` (
                    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'increment id',
                    `branch_id` BIGINT NOT NULL COMMENT 'branch transaction id',
                    `xid` VARCHAR(128) NOT NULL COMMENT 'global transaction id',
                    `context` VARCHAR(128) NOT NULL COMMENT 'undo log context',
                    `rollback_info` LONGBLOB NOT NULL COMMENT 'rollback info',
                    `log_status` INT NOT NULL COMMENT '0 normal, 1 global finished',
                    `log_created` DATETIME(6) NOT NULL COMMENT 'created time',
                    `log_modified` DATETIME(6) NOT NULL COMMENT 'modified time',
                    PRIMARY KEY (`id`),
                    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AT transaction mode undo table'
                """
            );
        }
    }
}
