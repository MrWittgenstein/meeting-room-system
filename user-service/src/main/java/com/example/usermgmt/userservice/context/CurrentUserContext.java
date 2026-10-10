package com.example.usermgmt.userservice.context;

import com.example.usermgmt.common.exception.BusinessException;
import java.util.List;

public final class CurrentUserContext {

    private static final ThreadLocal<String> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_ROLE_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<List<String>> USER_MENUS_HOLDER = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void set(String userId, String role, List<String> menus) {
        USER_ID_HOLDER.set(userId);
        USER_ROLE_HOLDER.set(role);
        USER_MENUS_HOLDER.set(menus == null ? List.of() : List.copyOf(menus));
    }

    public static String getUserId() {
        return USER_ID_HOLDER.get();
    }

    public static String requireUserId() {
        String userId = USER_ID_HOLDER.get();
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(401, "missing current user context");
        }
        return userId;
    }

    public static String getRole() {
        return USER_ROLE_HOLDER.get();
    }

    public static void requireAdmin() {
        String role = USER_ROLE_HOLDER.get();
        if (!"admin".equalsIgnoreCase(role)) {
            throw new BusinessException(40301, "missing admin role");
        }
    }

    public static boolean hasMenuPermission(String permissionCode) {
        List<String> menus = USER_MENUS_HOLDER.get();
        return menus != null && menus.contains(permissionCode);
    }

    public static void requireMenuPermission(String permissionCode) {
        requireUserId();
        if (!hasMenuPermission(permissionCode)) {
            throw new BusinessException(40301, "missing permission: " + permissionCode);
        }
    }

    public static boolean hasAnyMenuPermission(String... permissionCodes) {
        if (permissionCodes == null || permissionCodes.length == 0) {
            return false;
        }
        for (String permissionCode : permissionCodes) {
            if (hasMenuPermission(permissionCode)) {
                return true;
            }
        }
        return false;
    }

    public static void requireAnyMenuPermission(String... permissionCodes) {
        requireUserId();
        if (!hasAnyMenuPermission(permissionCodes)) {
            throw new BusinessException(40301, "missing permission");
        }
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
        USER_ROLE_HOLDER.remove();
        USER_MENUS_HOLDER.remove();
    }
}
