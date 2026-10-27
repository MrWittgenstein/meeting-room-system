package com.uestcfir.cache;

import java.util.concurrent.Callable;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;

/** A local Caffeine L1 in front of the shared Redis L2 cache. */
public final class TwoLevelCache implements Cache {
    private final String name;
    private final Cache l1;
    private final Cache l2;

    public TwoLevelCache(String name, Cache l1, Cache l2) {
        this.name = name;
        this.l1 = l1;
        this.l2 = l2;
    }

    @Override
    public String getName() { return name; }

    @Override
    public Object getNativeCache() { return this; }

    @Override
    public ValueWrapper get(Object key) {
        ValueWrapper first = l1.get(key);
        if (first != null) return first;
        ValueWrapper second;
        try {
            second = l2.get(key);
        } catch (RuntimeException ignored) {
            return null;
        }
        if (second != null) {
            l1.put(key, second.get());
        }
        return second;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        ValueWrapper wrapper = get(key);
        if (wrapper == null) return null;
        Object value = wrapper.get();
        if (type != null && value != null && !type.isInstance(value)) {
            throw new IllegalStateException("Cache value type mismatch for " + name);
        }
        return type == null ? (T) value : type.cast(value);
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) throws ValueRetrievalException {
        ValueWrapper first = l1.get(key);
        if (first != null) return (T) first.get();
        ValueWrapper second;
        try {
            second = l2.get(key);
        } catch (RuntimeException ignored) {
            second = null;
        }
        if (second != null) {
            l1.put(key, second.get());
            return (T) second.get();
        }
        try {
            T value = valueLoader.call();
            if (value != null) {
                l1.put(key, value);
                try { l2.put(key, value); } catch (RuntimeException ignored) { }
            }
            return value;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    public void put(Object key, Object value) {
        if (value == null) return;
        l1.put(key, value);
        try { l2.put(key, value); } catch (RuntimeException ignored) { }
    }

    @Override
    public void evict(Object key) {
        l1.evict(key);
        try { l2.evict(key); } catch (RuntimeException ignored) { }
    }

    @Override
    public void clear() {
        l1.clear();
        try { l2.clear(); } catch (RuntimeException ignored) { }
    }
}
