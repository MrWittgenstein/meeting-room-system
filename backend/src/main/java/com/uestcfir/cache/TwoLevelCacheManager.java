package com.uestcfir.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.beans.factory.InitializingBean;

public final class TwoLevelCacheManager implements CacheManager, InitializingBean {
    private final CacheManager l2Manager;
    private final Map<String, Duration> ttlByName;
    private final Map<String, Long> maxSizeByName;
    private final Map<String, Cache> caches = new ConcurrentHashMap<>();

    public TwoLevelCacheManager(CacheManager l2Manager, Map<String, Duration> ttlByName, long maxSize) {
        this.l2Manager = l2Manager;
        this.ttlByName = new LinkedHashMap<>(ttlByName);
        this.maxSizeByName = ttlByName.keySet().stream().collect(java.util.stream.Collectors.toMap(name -> name, name -> maxSize));
    }

    @Override
    public Cache getCache(String name) {
        if (!ttlByName.containsKey(name)) return null;
        return caches.computeIfAbsent(name, this::create);
    }

    @Override
    public java.util.Collection<String> getCacheNames() { return ttlByName.keySet(); }

    public CacheManager getL2Manager() { return l2Manager; }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (l2Manager instanceof InitializingBean initializingBean) {
            initializingBean.afterPropertiesSet();
        }
    }

    private Cache create(String name) {
        Cache l2 = l2Manager.getCache(name);
        if (l2 == null) return null;
        CaffeineCache l1 = new CaffeineCache(name, Caffeine.newBuilder()
                .expireAfterWrite(ttlByName.get(name))
                .maximumSize(maxSizeByName.get(name))
                .build());
        return new TwoLevelCache(name, l1, l2);
    }
}
