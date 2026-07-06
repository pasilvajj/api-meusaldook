package com.economy.finance.config;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserCacheEvictor {

    private static final String[] CACHE_NAMES = {
        "dashboard", "monthlySummary", "budgetGoals", "accounts", "accountList", "categories"
    };

    private final CacheManager cacheManager;

    public void evictUser(Long userId) {
        if (userId == null) {
            return;
        }
        String prefix = userId + ":";
        for (String cacheName : CACHE_NAMES) {
            try {
                evictStartingWith(cacheName, prefix);
            } catch (RuntimeException ex) {
                // Não falhar gravações se a limpeza de cache falhar (ex.: concorrência).
            }
        }
    }

    private void evictStartingWith(String cacheName, String prefix) {
        Cache cache = cacheManager.getCache(cacheName);
        if (!(cache instanceof CaffeineCache caffeineCache)) {
            return;
        }
        caffeineCache
                .getNativeCache()
                .asMap()
                .keySet()
                .removeIf(key -> key != null && key.toString().startsWith(prefix));
    }
}
