package com.uestcfir.logging;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class CommandAudit {
    private CommandAudit() { }

    public static void put(String key, Object value) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            attributes.getRequest().setAttribute(key, value);
        }
    }
}
