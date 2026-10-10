package com.example.usermgmt.common.enums;

import java.util.Arrays;

public enum UserStatus {
    ACTIVE(1),
    DISABLED(2),
    LOCKED(3),
    CANCELLED(4);

    private final int code;

    UserStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static UserStatus fromCode(Integer code) {
        return Arrays.stream(values())
            .filter(item -> item.code == code)
            .findFirst()
            .orElse(ACTIVE);
    }
}
