package com.uestcfir.filter;

import com.uestcfir.auth.CurrentUserContext;
import com.uestcfir.auth.RedisSessionService;
import com.uestcfir.auth.SessionService;
import com.uestcfir.auth.SessionUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Session authentication filter.
 *
 * The class name is retained so existing integrations and tests do not need
 * to change. The value after "Bearer" is now a Redis SessionId, not a JWT.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final List<String> WHITELIST_PATHS = Arrays.asList(
            "/user/changepassword",
            "/user/login",
            "/user/login/**",
            "/user/register",
            "/user/sendcode",
            "/approver/login",
            "/approver/login/**",
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
            // Device identity is validated by the device handler itself.
            "/iot/ws/device",
            "/error"
    );

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final SessionService sessionService;

    @Autowired
    public JwtAuthenticationFilter(RedisSessionService sessionService) {
        this.sessionService = sessionService;
    }

    /** Constructor retained for isolated legacy filter tests. */
    public JwtAuthenticationFilter() {
        this.sessionService = null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestPath = request.getRequestURI();
        if (isWhitelisted(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        String sessionId = resolveSessionId(request);
        if (sessionId == null || sessionId.isBlank()) {
            logger.warn("Missing SessionId for path: {}", requestPath);
            sendErrorResponse(response, "Missing or expired session");
            return;
        }

        if (sessionService == null) {
            sendErrorResponse(response, "Session authentication is unavailable");
            return;
        }

        final SessionUser sessionUser;
        try {
            sessionUser = sessionService.touch(sessionId);
        } catch (Exception e) {
            logger.warn("Session validation failed for path: {} - {}", requestPath, e.getMessage());
            sendErrorResponse(response, "Session not found or expired");
            return;
        }

        CurrentUserContext.set(sessionUser);
        request.setAttribute("UserId", sessionUser.getUserId());
        request.setAttribute("UserType", sessionUser.getUserType());
        request.setAttribute("Role", sessionUser.getRole());
        request.setAttribute("Permissions", sessionUser.getPermissions());
        var authorities = sessionUser.getPermissions().stream()
                .map(permission -> new SimpleGrantedAuthority("PERM_" + permission))
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(sessionUser, null, authorities));
        try {
            filterChain.doFilter(request, response);
        } finally {
            CurrentUserContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private String resolveSessionId(HttpServletRequest request) {
        String sessionId = request.getHeader("SessionId");
        if (sessionId != null && !sessionId.isBlank()) {
            return sessionId.trim();
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return request.getHeader("X-Session-Id");
    }

    private boolean isWhitelisted(String requestPath) {
        return WHITELIST_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, requestPath));
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format(
                "{\"code\": 401, \"message\": \"%s\", \"timestamp\": %d}",
                message, System.currentTimeMillis()
        ));
        response.getWriter().flush();
    }
}
