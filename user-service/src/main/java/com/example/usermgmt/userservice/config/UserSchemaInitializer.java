package com.example.usermgmt.userservice.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Adds the bridge column without changing the existing user-service schema. */
@Component
@RequiredArgsConstructor
public class UserSchemaInitializer {
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initialize() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'user'",
                Integer.class);
        if (tableCount == null || tableCount == 0) {
            return;
        }
        Integer columnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'user' AND column_name = 'legacy_user_id'",
                Integer.class);
        if (columnCount == null || columnCount == 0) {
            jdbcTemplate.execute("ALTER TABLE `user` ADD COLUMN legacy_user_id INT NULL COMMENT 'meetingroom legacy user id'");
        }
        Integer cipherColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'user' AND column_name = 'legacy_email_cipher'",
                Integer.class);
        if (cipherColumnCount == null || cipherColumnCount == 0) {
            jdbcTemplate.execute("ALTER TABLE `user` ADD COLUMN legacy_email_cipher VARCHAR(255) NULL COMMENT 'encrypted legacy meetingroom email'");
        }
        Integer hashColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'user' AND column_name = 'legacy_email_hash'",
                Integer.class);
        if (hashColumnCount == null || hashColumnCount == 0) {
            jdbcTemplate.execute("ALTER TABLE `user` ADD COLUMN legacy_email_hash VARCHAR(64) NULL COMMENT 'legacy meetingroom email login hash'");
        }
        jdbcTemplate.update("""
                UPDATE `user` SET legacy_email_hash = legacy_email_cipher
                WHERE legacy_email_hash IS NULL
                  AND legacy_email_cipher REGEXP '^[0-9A-Fa-f]{32}([0-9A-Fa-f]{32})?$'
                """);
        Integer indexCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'user' AND index_name = 'uk_user_legacy_user_id'",
                Integer.class);
        if (indexCount == null || indexCount == 0) {
            jdbcTemplate.execute("CREATE UNIQUE INDEX uk_user_legacy_user_id ON `user` (legacy_user_id)");
        }
    }
}
