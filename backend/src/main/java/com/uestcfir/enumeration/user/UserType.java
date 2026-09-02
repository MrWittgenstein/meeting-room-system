package com.uestcfir.enumeration.user;

/**
 * 用户类型枚举
 *
 * @author ylshen
 * @since 1.0.0
 */
public enum UserType {
    APPROVER(0, "会议室管理员"),
    USER(1, "普通用户"),
    ADMIN(2, "超级管理员");

    private final Integer code;
    private final String description;

    UserType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据code获取枚举
     */
    public static UserType getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (UserType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("无效的用户类型代码: " + code);
    }

    /**
     * 检查是否为会议室管理员
     */
    public boolean isMeetingRoomAdmin() {
        return this == APPROVER;
    }

    /**
     * 检查是否为普通用户
     */
    public boolean isNormalUser() {
        return this == USER;
    }

    /**
     * 检查是否为超级管理员
     */
    public boolean isSuperAdmin() {
        return this == ADMIN;
    }

    /**
     * 检查是否有管理权限
     */
    public boolean hasAdminPermission() {
        return this == APPROVER || this == ADMIN;
    }

    /**
     * 获取所有管理员的code列表
     */

    @Override
    public String toString() {
        return this.description + "(" + this.code + ")";
    }
}