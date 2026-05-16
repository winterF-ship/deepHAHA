package com.forum.service;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CaptchaStore {

    private static class CaptchaEntry {
        String code;
        long expireTime;
        CaptchaEntry(String code, long expireTime) {
            this.code = code;
            this.expireTime = expireTime;
        }
    }

    private final ConcurrentHashMap<String, CaptchaEntry> map = new ConcurrentHashMap<>();

    public void put(String id, String code) {
        map.put(id, new CaptchaEntry(code, System.currentTimeMillis() + 5 * 60 * 1000));
    }

    public String getAndRemove(String id) {
        CaptchaEntry entry = map.remove(id);
        if (entry == null) return null;
        if (System.currentTimeMillis() > entry.expireTime) return null;
        return entry.code;
    }
}
