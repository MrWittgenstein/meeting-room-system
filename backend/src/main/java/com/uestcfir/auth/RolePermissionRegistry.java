package com.uestcfir.auth;

import com.uestcfir.enumeration.user.UserType;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Compatibility RBAC registry. The existing user_type column is treated as
 * the role assignment source until user_role/role_permission tables are
 * migrated. Controllers only depend on permission codes.
 */
public final class RolePermissionRegistry {
    public static final String ROOM_READ = "room:read";
    public static final String ROOM_MANAGE = "room:manage";
    public static final String RESERVATION_CREATE = "reservation:create";
    public static final String RESERVATION_READ_SELF = "reservation:read:self";
    public static final String RESERVATION_CANCEL_SELF = "reservation:cancel:self";
    public static final String RESERVATION_APPROVE = "reservation:approve";
    public static final String USER_READ_SELF = "user:read:self";
    public static final String USER_UPDATE_SELF = "user:update:self";
    public static final String USER_MANAGE = "user:manage";
    public static final String NEWS_READ = "news:read";
    public static final String NEWS_MANAGE = "news:manage";
    public static final String STATISTICS_READ = "statistics:read";
    public static final String IOT_READ = "iot:read";
    public static final String IOT_CONTROL = "iot:control";

    private RolePermissionRegistry() {
    }

    public static String roleOf(Integer userType) {
        if (UserType.ADMIN.getCode().equals(userType)) {
            return "super_admin";
        }
        if (UserType.APPROVER.getCode().equals(userType)) {
            return "room_admin";
        }
        return "user";
    }

    public static String displayUserType(String role, Integer legacyUserType) {
        if ("super_admin".equalsIgnoreCase(role)) {
            return UserType.ADMIN.getDescription();
        }
        if ("room_admin".equalsIgnoreCase(role)) {
            return UserType.APPROVER.getDescription();
        }
        UserType type = UserType.getByCode(legacyUserType);
        return type == null ? UserType.USER.getDescription() : type.getDescription();
    }

    public static Set<String> permissionsOf(Integer userType) {
        Set<String> permissions = new LinkedHashSet<>();
        permissions.add(ROOM_READ);
        permissions.add(RESERVATION_READ_SELF);
        permissions.add(USER_READ_SELF);
        permissions.add(USER_UPDATE_SELF);
        permissions.add(IOT_READ);

        if (UserType.USER.getCode().equals(userType)) {
            permissions.add(RESERVATION_CREATE);
            permissions.add(RESERVATION_CANCEL_SELF);
            return permissions;
        }

        permissions.add(ROOM_MANAGE);
        permissions.add(RESERVATION_APPROVE);
        permissions.add(NEWS_READ);
        permissions.add(NEWS_MANAGE);
        permissions.add(STATISTICS_READ);
        permissions.add(IOT_CONTROL);
        permissions.add(USER_MANAGE);
        return permissions;
    }
}
