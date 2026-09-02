package com.uestcfir.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 邮箱加密服务
 * 使用固定盐值加密，解密时不需要原始邮箱
 */
@Service
public class EmailEncryptionService {

    @Value("${app.master.key:123456}")
    private String masterKey;

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    /**
     * 加密邮箱
     * @param email 原始邮箱
     * @return 加密后的字符串
     */
    public String encryptEmail(String email) {
        try {
            validateInput(email);

            String normalizedEmail = normalizeEmail(email);
            SecretKeySpec secretKey = getSecretKey();

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(normalizedEmail.getBytes());

            return Base64.getEncoder().encodeToString(encryptedBytes);

        } catch (Exception e) {
            throw new RuntimeException("邮箱加密失败: " + email, e);
        }
    }

    /**
     * 解密邮箱
     * @param encryptedEmail 加密后的邮箱字符串
     * @return 解密后的原始邮箱
     */
    public String decryptEmail(String encryptedEmail) {
        try {
            validateInput(encryptedEmail);

            SecretKeySpec secretKey = getSecretKey();

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedEmail));

            return new String(decryptedBytes);

        } catch (Exception e) {
            throw new RuntimeException("邮箱解密失败: " + encryptedEmail, e);
        }
    }

    /**
     * 从主密钥创建符合长度的SecretKey
     */
    private SecretKeySpec getSecretKey() {
        try {
            // 使用SHA-256哈希将短密钥扩展为32字节
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(masterKey.getBytes());
            return new SecretKeySpec(keyBytes, ALGORITHM);
        } catch (Exception e) {
            throw new RuntimeException("密钥创建失败", e);
        }
    }

    /**
     * 计算邮箱哈希值（用于快速查询和唯一性校验）
     * @param email 邮箱地址
     * @return MD5哈希值
     */
    public String computeEmailHash(String email) {
        try {
            validateInput(email);

            String normalizedEmail = normalizeEmail(email);
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hashBytes = digest.digest(normalizedEmail.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (Exception e) {
            throw new RuntimeException("哈希计算失败", e);
        }
    }

    /**
     * 验证邮箱格式（基础验证）
     * @param email 邮箱地址
     * @return 是否符合基本邮箱格式
     */
    public boolean isValidEmailFormat(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }

        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    /**
     * 验证加密数据是否有效
     * @param encryptedEmail 加密后的邮箱
     * @return 是否为有效的加密数据
     */
    public boolean isValidEncryptedEmail(String encryptedEmail) {
        if (!StringUtils.hasText(encryptedEmail)) {
            return false;
        }

        try {
            // 尝试Base64解码
            Base64.getDecoder().decode(encryptedEmail);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证输入参数
     */
    private void validateInput(String input) {
        if (!StringUtils.hasText(input)) {
            throw new IllegalArgumentException("输入不能为空");
        }

        if (masterKey == null || masterKey.trim().isEmpty()) {
            throw new IllegalStateException("主密钥未配置");
        }
    }

    /**
     * 规范化邮箱（转为小写并去除空格）
     */
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    /**
     * 验证加密配置
     */
    public void validateConfig() {
        if (masterKey == null || masterKey.trim().isEmpty()) {
            throw new IllegalStateException("主密钥未配置，请检查 app.master.key 配置");
        }

        System.out.println("✅ 邮箱加密配置验证通过");
        System.out.println("   主密钥: " + maskKey(masterKey));
        System.out.println("   加密算法: " + ALGORITHM);
        System.out.println("   加密模式: " + TRANSFORMATION);
    }

    /**
     * 隐藏密钥显示（安全考虑）
     */
    private String maskKey(String key) {
        if (key.length() <= 2) {
            return "***";
        }
        return key.substring(0, 2) + "***" + key.substring(key.length() - 2);
    }

    /**
     * 完整的加密解密测试
     */
    public void testEncryption() {
        try {
            System.out.println("🔐 开始邮箱加密测试...");

            String testEmail = "test@example.com";
            System.out.println("   测试邮箱: " + testEmail);

            // 验证邮箱格式
            boolean validFormat = isValidEmailFormat(testEmail);
            System.out.println("   邮箱格式验证: " + (validFormat ? "✅ 通过" : "❌ 失败"));

            // 计算哈希
            String emailHash = computeEmailHash(testEmail);
            System.out.println("   邮箱哈希: " + emailHash);

            // 加密
            String encrypted = encryptEmail(testEmail);
            System.out.println("   加密结果: " + encrypted);

            // 验证加密数据
            boolean validEncrypted = isValidEncryptedEmail(encrypted);
            System.out.println("   加密数据验证: " + (validEncrypted ? "✅ 通过" : "❌ 失败"));

            // 解密
            String decrypted = decryptEmail(encrypted);
            System.out.println("   解密结果: " + decrypted);

            // 验证一致性
            boolean success = testEmail.equals(decrypted);
            System.out.println("   加密解密一致性: " + (success ? "✅ 通过" : "❌ 失败"));

            if (success) {
                System.out.println("🎉 所有测试通过！邮箱加密服务正常工作");
            } else {
                System.out.println("💥 测试失败！请检查加密配置");
            }

        } catch (Exception e) {
            System.out.println("💥 加密测试异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 批量测试多个邮箱
     */
    public void batchTest() {
        String[] testEmails = {
                "user1@example.com",
                "admin@company.cn",
                "test.user+tag@gmail.com",
                "john.doe@sub.domain.org"
        };

        System.out.println("🧪 开始批量邮箱加密测试...");
        int successCount = 0;

        for (String email : testEmails) {
            try {
                String encrypted = encryptEmail(email);
                String decrypted = decryptEmail(encrypted);

                if (email.equals(decrypted)) {
                    System.out.println("   ✅ " + email + " → 加密解密成功");
                    successCount++;
                } else {
                    System.out.println("   ❌ " + email + " → 加密解密失败");
                }
            } catch (Exception e) {
                System.out.println("   💥 " + email + " → 异常: " + e.getMessage());
            }
        }

        System.out.println("📊 批量测试结果: " + successCount + "/" + testEmails.length + " 通过");
    }
}