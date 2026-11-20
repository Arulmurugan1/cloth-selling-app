package com.clothapp.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class SessionLimiterService {

    private final StringRedisTemplate redis;
    private final int maxActiveUsers;

    public SessionLimiterService(StringRedisTemplate redis,
                                 @Value("${app.concurrent-login-limit:2}") int maxActiveUsers) {
        this.redis = redis;
        this.maxActiveUsers = maxActiveUsers;
    }

    public boolean registerSession(String userId, long ttlSeconds) {
        Long active = redis.opsForSet().size("auth:active-users");
        if (active != null && active >= maxActiveUsers) {
            return false;
        }
        String tokenId = userId + ":" + Instant.now().toEpochMilli();
        redis.opsForSet().add("auth:active-users", userId);
        redis.opsForValue().set("auth:session:" + tokenId, userId, Duration.ofSeconds(ttlSeconds));
        redis.expire("auth:active-users", Duration.ofSeconds(ttlSeconds));
        return true;
    }

    public void removeSession(String userId) {
        redis.opsForSet().remove("auth:active-users", userId);
    }
}
