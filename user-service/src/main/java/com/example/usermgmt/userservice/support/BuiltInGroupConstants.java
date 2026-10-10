package com.example.usermgmt.userservice.support;

import java.util.List;

public final class BuiltInGroupConstants {

    public static final String ADMIN_GROUP_ID = "group-admin";
    public static final String NORMAL_USER_GROUP_ID = "group-normal-user";
    public static final String NORMAL_USER_GROUP_NAME = "\u666e\u901a\u7528\u6237\u7ec4";

    public static final List<String> NORMAL_USER_PERMISSION_CODES = List.of(
        "menu:dashboard",
        "menu:device:sessions",
        "api:user:updateMe",
        "api:user:changePassword",
        "api:user:attribute:list",
        "api:user:attribute:create",
        "api:user:attribute:update",
        "api:user:attribute:delete",
        "api:user:permission:effective",
        "api:user:lookup",
        "api:message:list",
        "api:message:read",
        "api:message:send",
        "api:message:template:list",
        "api:message:template:preview",
        "api:group:listPublic",
        "api:group:listMy",
        "api:group:detailMy",
        "api:group:join"
    );

    private BuiltInGroupConstants() {
    }
}
