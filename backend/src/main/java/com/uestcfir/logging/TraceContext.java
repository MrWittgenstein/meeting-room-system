package com.uestcfir.logging;

public final class TraceContext {
    public static final String HEADER = "X-Trace-Id";
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TraceContext() {
    }

    public static String get() {
        return CURRENT.get();
    }

    public static void set(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            clear();
        } else {
            CURRENT.set(traceId.trim());
        }
    }

    public static void clear() {
        CURRENT.remove();
    }
}
