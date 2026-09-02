package com.uestcfir.filter;

import com.uestcfir.utils.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * JWT 认证过滤器
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final List<String> WHITELIST_PATHS = Arrays.asList(
            "/user/changepassword",
            "/user/login",
            "/user/register",
            "/user/sendcode",
            "/user/login/password",
            "/approver/login",
            "/approver/login/password",
            "/approver/register",
            "/approver/sendcode",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/",
            "/index.html",
            "/styles/**",
            "/scripts/**",
            "/assets/**",
            "/vendor/**",
            "/iot/**",
            "/iot/ws/**",
            "/ws/**",
            "/error"

    );

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 放行OPTIONS请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestPath = request.getRequestURI();

        // 白名单检查
        if (isWhitelisted(requestPath)) {
            logger.debug("Whitelisted path accessed: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        // Token验证
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Missing or invalid Authorization header for path: {}", requestPath);
            sendErrorResponse(response, "Missing or invalid token");
            return;
        }

        try {

            Integer[] userInfo = JwtUtil.validateToken(authHeader);

            logger.debug("User authenticated with UserId: {}, UserType: {}", userInfo[0], userInfo[1]);
            request.setAttribute("UserId", userInfo[0]);
            request.setAttribute("UserType", userInfo[1]);

        } catch (JwtException e) {
            logger.warn("JWT validation failed for path: {} - {}", requestPath, e.getMessage());
            sendErrorResponse(response, "Invalid token: " + e.getMessage());
            return;
        } catch (Exception e) {
            logger.error("Unexpected error during JWT validation for path: {}", requestPath, e);
            sendErrorResponse(response, "Authentication error");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isWhitelisted(String requestPath) {
        return WHITELIST_PATHS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, requestPath));
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        String jsonResponse = String.format(
                "{\"code\": 401, \"message\": \"%s\", \"timestamp\": %d}",
                message, System.currentTimeMillis()
        );

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}
