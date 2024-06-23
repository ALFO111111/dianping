package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    //2024-5-8 0:0:0的秒数
    private static final long BEGIN_TIMESTAMP = 1715126400L;
    private static final int COUNT_BITS = 32;

    public long nextId(String keyPrefix) {
        //1.生成时间戳
        long timeStamp = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) - BEGIN_TIMESTAMP;
        //2.生成序列号
        //这里为了避免超过范围，分为每一天，每一天不可能超过2^32
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);
        //3.拼接并返回
        //由于是64位，低32位是序列号，所以将时间戳左移32位
        //由于左移后，低32位全0，直接或比加更快
        return timeStamp << COUNT_BITS | count;
    }

}
