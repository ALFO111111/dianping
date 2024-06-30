package com.alfo;

import cn.hutool.json.JSONUtil;

import com.alfo.user.domain.po.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Random;

//@SpringBootTest
public class test {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void stringRedisTemplateTest() {
        User user = User.builder()
                .id(1L)
                .phone("15458796854")
                .nickName("alfo")
                .build();
        String userJson = JSONUtil.toJsonStr(user);
        stringRedisTemplate.opsForValue().set("user:1", userJson);
        System.out.println(JSONUtil.toBean(stringRedisTemplate.opsForValue().get("user:1"), User.class));
    }

    @Test
    public void eatPlace() {
        String[] dests = {"金贵二楼", "西北餐厅", "紫藤二楼", "米线"};

        Random random = new Random();
        System.out.println(dests[random.nextInt(dests.length)]);
    }
}
