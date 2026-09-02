package com.uestcfir.enumeration.user;

public enum UserStatus {
    NORMAL(0, "正常"),
    FROZEN(1, "冻结");

    private final Integer code;
    private final String description;

    UserStatus(Integer code, String description) {
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
    public static UserStatus getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (UserStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的用户状态代码: " + code);
    }

    /**
     * 检查是否为正常状态
     */
    public boolean isNormal() {
        return this == NORMAL;
    }

    /**
     * 检查是否为冻结状态
     */
    public boolean isFrozen() {
        return this == FROZEN;
    }

    /**
     * 检查用户是否可用（正常状态）
     */
    public boolean isAvailable() {
        return this == NORMAL;
    }

    @Override
    public String toString() {
        return this.description + "(" + this.code + ")";
    }
}