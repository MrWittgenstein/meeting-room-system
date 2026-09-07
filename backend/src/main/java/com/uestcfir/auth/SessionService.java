package com.uestcfir.auth;

import com.uestcfir.pojo.entity.User;

public interface SessionService {
    SessionUser create(User user);

    SessionUser get(String sessionId);

    SessionUser touch(String sessionId);

    void invalidate(String sessionId);
}
