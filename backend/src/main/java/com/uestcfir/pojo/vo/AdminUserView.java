package com.uestcfir.pojo.vo;

import com.uestcfir.pojo.entity.User;

/** Administrative display data; unreadable legacy email never leaves as ciphertext. */
public class AdminUserView extends User {
    private boolean emailUnavailable;

    public boolean isEmailUnavailable() { return emailUnavailable; }
    public void setEmailUnavailable(boolean emailUnavailable) { this.emailUnavailable = emailUnavailable; }
}
