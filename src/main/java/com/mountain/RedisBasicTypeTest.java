package com.mountain;

import lombok.SneakyThrows;
import org.springframework.data.redis.core.RedisTemplate;
import redis.clients.jedis.Jedis;

import java.util.concurrent.TimeUnit;

public class RedisBasicTypeTest {

    public static void main(String[] args) {
        printString();
    }

    @SneakyThrows
    public static void printString() {
        Jedis jedis = new Jedis("192.168.1.120", 6379);
        jedis.flushDB();
        // 关于string
        System.out.println("step1 : " + jedis.set("k2", "v2"));
        System.out.println("step2 : " + jedis.get("k2"));
        System.out.println("step3 : " + jedis.setnx("k2", "v3"));
        System.out.println("step4 : " + jedis.strlen("k2"));
        System.out.println("step5 : " + jedis.exists("k2"));
        System.out.println("step6 : " + jedis.set("k3", "10"));
        System.out.println("step7 : " + jedis.incr("k3"));
        System.out.println("step8 : " + jedis.decr("k3"));
        System.out.println("step9 : " + jedis.incrBy("k3", 100));
        System.out.println("step10 : " + jedis.mset("k4", "v4", "k5", "v5"));
        System.out.println("step11 : " + jedis.msetnx("k5", "v5", "k6", "v6"));

        jedis.expire("k5", 3);
        for (int i = 0; i < 3; i++) {
            if (jedis.get("k5") != null) {
                System.out.println("step12: k5还未过期，值是: " + jedis.get("k5"));
            } else {
                System.out.println("step12: k5过期");
            }

            Thread.sleep(2000);
        }

    }


}
