package com.example.usermgmt.userservice.context;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.usermgmt.userservice.entity.UserGroupEntity;
import com.example.usermgmt.userservice.mapper.UserGroupMapper;
import com.example.usermgmt.userservice.support.BuiltInGroupConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class CurrentUserInterceptor implements HandlerInterceptor {

    private final UserGroupMapper userGroupMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = request.getHeader("X-User-Id");
        String role = resolveRole(userId, request.getHeader("X-User-Role"));
        CurrentUserContext.set(userId, role, parseMenus(request.getHeader("X-User-Menus")));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUserContext.clear();
    }

    private String resolveRole(String userId, String fallbackRole) {
        if (userId == null || userId.isBlank()) {
            return fallbackRole;
        }
        QueryWrapper<UserGroupEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId).eq("group_id", BuiltInGroupConstants.ADMIN_GROUP_ID);
        return userGroupMapper.selectCount(queryWrapper) > 0 ? "admin" : "user";
    }

    private List<String> parseMenus(String rawMenus) {
        if (rawMenus == null || rawMenus.isBlank()) {
            return List.of();
        }
        return Arrays.stream(rawMenus.split(","))
            .map(String::trim)
            .filter(menu -> !menu.isBlank())
            .distinct()
            .toList();
    }
}
