package com.example.usermgmt.userservice.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class PasswordCodec {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordCodec() {
    }

    public static String generateSalt() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public static String encode(String plainText, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest((salt + plainText).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not supported", exception);
        }
    }

    public static boolean matches(String plainText, String salt, String encoded) {
        return encode(plainText, salt).equals(encoded);
    }
}
