package com.clothapp.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class LoginAttemptService {

    private final StringRedisTemplate redis;
    private final int maxFailed;

    public LoginAttemptService(StringRedisTemplate redis,
                               @Value("${app.login.max-failed-attempts:5}") int maxFailed) {
        this.redis = redis;
        this.maxFailed = maxFailed;
    }

    public void checkAllowed(String login) {
        String key = "auth:failed:" + login.toLowerCase();
        String value = redis.opsForValue().get(key);
        if (value != null && Integer.parseInt(value) >= maxFailed) {
            throw new SecurityException("Too many failed login attempts. Try again later.");
        }
    }

    public void success(String login) {
        redis.delete("auth:failed:" + login.toLowerCase());
    }

    public void fail(String login) {
        String key = "auth:failed:" + login.toLowerCase();
        Long c = redis.opsForValue().increment(key);
        if (c != null && c == 1L) {
            redis.expire(key, Duration.ofMinutes(15));
        }
    }
}
