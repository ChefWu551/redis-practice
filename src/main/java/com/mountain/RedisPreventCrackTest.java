package com.mountain;

import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

public class RedisPreventCrackTest {

    public static void preventCrack() {
        RedisTemplate template = new RedisTemplate();
        template.opsForValue().setIfAbsent("k1", "v1", 3, TimeUnit.SECONDS);
    }
}
