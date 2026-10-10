package com.example.usermgmt.userservice.migration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/** One-time, repeatable migration of encrypted meeting-room email accounts. */
public final class LegacyMeetingEmailBackfill {

    private LegacyMeetingEmailBackfill() {
    }

    public static void main(String[] args) throws Exception {
        String masterKey = env("MEETINGROOM_MASTER_KEY", "");
        String sourceUrl = env("MEETING_DB_URL",
                "jdbc:mysql://127.0.0.1:3306/meetingroom?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC");
        String targetUrl = env("USER_DB_URL",
                "jdbc:mysql://127.0.0.1:3306/meetingroom_users?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai");
        String sourceUser = env("MEETING_DB_USERNAME", "root");
        String sourcePassword = requiredEnv("MEETING_DB_PASSWORD");
        String targetUser = env("USER_DB_USERNAME", "root");
        String targetPassword = requiredEnv("USER_DB_PASSWORD");

        List<EmailRecord> records;
        try (Connection source = DriverManager.getConnection(sourceUrl, sourceUser, sourcePassword)) {
            records = loadAndDecrypt(source, masterKey);
        }

        Set<String> emails = new HashSet<>();
        for (EmailRecord record : records) {
            if (!emails.add(record.storedHash().toLowerCase(Locale.ROOT))) {
                throw new IllegalStateException("Duplicate normalized legacy email; no target rows were updated");
            }
        }

        int updated;
        try (Connection target = DriverManager.getConnection(targetUrl, targetUser, targetPassword)) {
            ensureTargetColumn(target, "legacy_email_cipher", "VARCHAR(255) NULL");
            ensureTargetColumn(target, "legacy_email_hash", "VARCHAR(64) NULL");
            target.setAutoCommit(false);
            try {
                validateTargetsAndConflicts(target, records);
                updated = updateTarget(target, records);
                target.commit();
            } catch (Exception exception) {
                target.rollback();
                throw exception;
            }
        }
        System.out.println("Legacy emails verified and backfilled: " + updated);
    }

    private static void ensureTargetColumn(Connection target, String column, String definition) throws Exception {
        try (ResultSet columns = target.getMetaData().getColumns(null, null, "user", column)) {
            if (!columns.next()) {
                try (var statement = target.createStatement()) {
                    statement.execute("ALTER TABLE `user` ADD COLUMN `" + column + "` " + definition);
                }
            }
        }
    }

    private static List<EmailRecord> loadAndDecrypt(Connection source, String masterKey) throws Exception {
        List<EmailRecord> records = new ArrayList<>();
        String sql = "SELECT user_id, email, email_hash FROM `user` WHERE email IS NOT NULL AND email <> ''";
        try (PreparedStatement statement = source.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                int legacyUserId = rows.getInt("user_id");
                String cipher = rows.getString("email");
                String storedHash = rows.getString("email_hash");
                if (storedHash == null || !storedHash.matches("(?i)^[0-9a-f]{32}([0-9a-f]{32})?$")) {
                    throw new IllegalStateException("Legacy email hash is invalid for user id " + legacyUserId);
                }
                String email = null;
                if (masterKey != null && !masterKey.isBlank()) {
                    try {
                        String decrypted = decrypt(cipher, masterKey).trim().toLowerCase(Locale.ROOT);
                        if (decrypted.contains("@") && hashMatches(decrypted, storedHash)) {
                            email = decrypted;
                        } else {
                            System.err.println("Decrypted email did not match its legacy hash for user " + legacyUserId
                                    + "; password login hash will be preserved");
                        }
                    } catch (Exception exception) {
                        System.err.println("Could not decrypt legacy email for user " + legacyUserId
                                + "; encrypted value and login hash will be preserved");
                    }
                }
                records.add(new EmailRecord(legacyUserId, email, cipher, storedHash));
            }
        }
        return records;
    }

    private static void validateTargetsAndConflicts(Connection target, List<EmailRecord> records) throws Exception {
        String targetSql = "SELECT id, mail FROM `user` WHERE legacy_user_id = ?";
        String conflictSql = "SELECT legacy_user_id FROM `user` WHERE mail = ? AND legacy_user_id <> ? LIMIT 1";
        try (PreparedStatement targetQuery = target.prepareStatement(targetSql);
             PreparedStatement conflictQuery = target.prepareStatement(conflictSql)) {
            for (EmailRecord record : records) {
                targetQuery.setInt(1, record.legacyUserId());
                try (ResultSet row = targetQuery.executeQuery()) {
                    if (!row.next()) {
                        throw new IllegalStateException("No migrated target user for legacy id " + record.legacyUserId());
                    }
                }
                if (record.email() != null) {
                    conflictQuery.setString(1, record.email());
                    conflictQuery.setInt(2, record.legacyUserId());
                    try (ResultSet conflict = conflictQuery.executeQuery()) {
                        if (conflict.next()) {
                            throw new IllegalStateException("Email conflicts with another user; no target rows were updated");
                        }
                    }
                }
            }
        }
    }

    private static int updateTarget(Connection target, List<EmailRecord> records) throws Exception {
        String sql = "UPDATE `user` SET mail = ?, legacy_email_cipher = ?, legacy_email_hash = ? WHERE legacy_user_id = ?";
        int updated = 0;
        try (PreparedStatement statement = target.prepareStatement(sql)) {
            for (EmailRecord record : records) {
                statement.setString(1, record.email());
                statement.setString(2, record.cipher());
                statement.setString(3, record.storedHash());
                statement.setInt(4, record.legacyUserId());
                updated += statement.executeUpdate();
            }
        }
        return updated;
    }

    private static String decrypt(String encrypted, String masterKey) throws Exception {
        byte[] key = MessageDigest.getInstance("SHA-256").digest(masterKey.getBytes(StandardCharsets.UTF_8));
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
        return new String(cipher.doFinal(Base64.getDecoder().decode(encrypted)), StandardCharsets.UTF_8);
    }

    private static String emailHash(String email) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(email.getBytes(StandardCharsets.UTF_8)));
    }

    private static boolean hashMatches(String email, String storedHash) throws Exception {
        String md5 = emailHash(email);
        String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(email.getBytes(StandardCharsets.UTF_8)));
        return md5.equalsIgnoreCase(storedHash) || sha256.equalsIgnoreCase(storedHash);
    }

    private static String requiredEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required environment variable is missing: " + key);
        }
        return value;
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private record EmailRecord(int legacyUserId, String email, String cipher, String storedHash) {
    }
}
