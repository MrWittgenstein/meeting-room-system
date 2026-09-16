package com.uestcfir.auth;

import com.uestcfir.exception.BusinessException;

public final class CurrentUserContext {
    private static final ThreadLocal<SessionUser> CURRENT = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void set(SessionUser user) {
        CURRENT.set(user);
    }

    public static SessionUser get() {
        return CURRENT.get();
    }

    public static SessionUser requireUser() {
        SessionUser user = CURRENT.get();
        if (user == null) {
            throw new BusinessException("未登录或会话已过期");
        }
        return user;
    }

    public static Integer requireUserId() {
        return requireUser().getUserId();
    }

    public static void requirePermission(String permission) {
        SessionUser user = requireUser();
        if (user.getPermissions() == null || !user.getPermissions().contains(permission)) {
            throw new BusinessException("无权限执行该操作");
        }
    }

    public static boolean hasPermission(String permission) {
        SessionUser user = CURRENT.get();
        return user != null && user.getPermissions() != null && user.getPermissions().contains(permission);
    }

    public static void clear() {
        CURRENT.remove();
    }
}
