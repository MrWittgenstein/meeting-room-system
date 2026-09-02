package com.uestcfir.utils;



import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;


import java.util.Date;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JWT工具类
 *
 * @author ylshen
 * @since 1.0.0
 */
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    private static final String SECRET = System.getenv("JWT_SECRET") != null ? System.getenv("JWT_SECRET") : "default_secret";


    
    /**
     * 生成 JWT Token
     *
     * @param claims 包含用户信息的键值对
     * @return 生成的 Token
     */
    public static String generateToken(Map<String,Object> claims){
        String token = Jwts.builder()
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .addClaims(claims)
                
                .compact();
        logger.info("Generated JWT token for claims: {}", claims);
        return token;
    }

    /**
     * 解析 JWT Token 获取 Claims
     *
     * @param token JWT Token
     * @return 包含用户信息的键值对
     * @throws IllegalArgumentException 如果 Token 无效
     */
    public static Map<String,Object> getClaims(String token){
        try {
            Map<String, Object> claims = Jwts.parser()
                    .setSigningKey(SECRET)
                    .parseClaimsJws(token)
                    .getBody();
            logger.debug("Successfully parsed JWT token: {}", token);
            return claims;
        } catch (Exception e) {
            logger.error("Failed to parse JWT token: {}", token, e);
            throw e;
        }
    }
    /**
     * 验证 Token 并提取用户信息
     *
     * @param authHeader 认证头信息
     * @return 用户ID和用户类型数组
     * @throws IllegalArgumentException 如果 Token 无效
     */
    public static Integer[] validateToken(String authHeader){
        String token = authHeader.substring(7);//去掉前缀Bearer,从第七位开始截取token
        logger.debug("Validating JWT token: {}", token);
        try {
            Map<String, Object> claims = JwtUtil.getClaims(token);
            Integer userId = (Integer) claims.get("UserId");
            Integer userType = (Integer) claims.get("UserType");
            if (userId == null || userType == null) {
                logger.error("Invalid token: missing UserId or UserType");
                throw new IllegalArgumentException("token不合规");
            }
            logger.info("Successfully validated token for UserId: {}, UserType: {}", userId, userType);
            return new Integer[]{userId,userType};
        } catch (Exception e) {
            logger.error("Token validation failed: {}", token, e);
            throw e;
        }
    }

    /**
     * 刷新 Token（如果即将过期）
     *
     * @param token 当前 Token
     * @return 新的 Token（如果不需要刷新则返回 null）
     */
    public static String refreshToken(String token) {
        Map<String, Object> claims = getClaims(token);
        Date expiration = (Date) claims.get("exp");
        long remainingTime = expiration.getTime() - System.currentTimeMillis();
        if (remainingTime < 30 * 60 * 1000) { // 剩余时间小于30分钟
            return generateToken(claims);
        }
        return null;
    }

}


