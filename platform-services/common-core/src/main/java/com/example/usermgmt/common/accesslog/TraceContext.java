package com.example.usermgmt.common.accesslog;

import org.springframework.util.StringUtils;

public final class TraceContext {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    private TraceContext() {
    }

    public static String getTraceId() {
        return TRACE_ID.get();
    }

    public static void setTraceId(String traceId) {
        if (StringUtils.hasText(traceId)) {
            TRACE_ID.set(traceId.trim());
            return;
        }
        clear();
    }

    public static void clear() {
        TRACE_ID.remove();
    }
}
