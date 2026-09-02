package com.uestcfir.enumeration.reservation;

import com.uestcfir.enumeration.user.UserStatus;

/**
 * 预定状态枚举
 *
 * @author ylshen
 * @since 1.0.0
 */
public enum ReservationStatus {

    WAITING_CONFIRM(0, "待确认"),
    CANCELED(1, "已拒绝"),
    USING(3, "使用中"),
    EXPIRED(2, "已过期"),
    APPROVED(4, "已通过");


    private final Integer code;
    private final String description;

    ReservationStatus (Integer code, String description) {
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
    public static ReservationStatus getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ReservationStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的用户状态代码: " + code);
    }



    @Override
    public String toString() {
        return this.description + "(" + this.code + ")";
    }
}
