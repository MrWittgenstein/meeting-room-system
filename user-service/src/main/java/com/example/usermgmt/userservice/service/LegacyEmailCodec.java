package com.example.usermgmt.userservice.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class LegacyEmailCodec {
    public String sha256(String email) {
        return hash(email, "SHA-256");
    }

    public String md5(String email) {
        return hash(email, "MD5");
    }

    private String hash(String email, String algorithm) {
        if (!StringUtils.hasText(email)) {
            return "";
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm)
                    .digest(email.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("email hash unavailable", exception);
        }
    }
}
