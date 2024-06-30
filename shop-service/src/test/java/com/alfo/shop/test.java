package com.alfo.shop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
public class test {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void startTest() {
        String status = stringRedisTemplate.opsForValue().get("shop:status");
        System.out.println("status:" + status);
    }

}
