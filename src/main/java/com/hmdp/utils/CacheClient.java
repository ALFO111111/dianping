package com.hmdp.utils;


import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.sql.Time;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Component
public class CacheClient {

    private static StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(10);


    //将任意Java对象序列化为Json并存储在String类型的key中，同时设置TTL过期时间
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    //将任意Java对象序列化为Json并存储在String类型的key中，并且可以设置逻辑过期时间，用于处理缓存击穿的问题
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        RedisData redisData = new RedisData();
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        redisData.setData(value);
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value));
    }

    //根据指定的key查询缓存，并反序列化为指定类型，利用缓存控制方式解决缓存穿透问题
    //缓存穿透：查询没有的数据，一直打到数据库上
    public <R, ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> classType,
                                          Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        //判断缓存是否命中
        if (json != null) {
            //如果命中则：判断是否为空值
            //为空值，结束
            if (json.isEmpty()) {
                return null;
            }
            //不为空，返回商铺信息
            return JSONUtil.toBean(json, classType);
        }
        //未命中，查询数据库
        //这里我们不知道也不可能知道，所以有使用者自己传递
        R r = dbFallback.apply(id);
        //存在：将商铺写入Redis，返回
        if (r != null) {
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(r), time, unit);
            return r;
        }
        //不存在，将空值写入Redis，结束
        stringRedisTemplate.opsForValue().set(key, "", time, unit);
        return null;
    }

    //根据指定的key查询缓存，并反序列化为指定类型，需要利用逻辑过期解决缓存击穿问题
    //缓存击穿：某个经常访问的key，突然失效，导致大批访问SQL数据库（不存在不存在的情况）
    //解决方案是逻辑过期，但是缓存中必须有该value，通过@Test生成
    public <R, ID> R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> classType,
                                            Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        //1.根据id查询缓存
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        //2.判断是否命中
        //2.1未命中，返回空
        if (StrUtil.isBlank(json)) {
            return null;
        }
        //2.2命中，判断是否过期
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), classType);
        //3.未过期，返回商铺信息
        if (!LocalDateTime.now().isAfter(redisData.getExpireTime())) {
            return r;
        }
        //3.1过期，获取锁（锁是通过setex/setIfAbsent实现）
        //4.获取锁失败，返回旧的商铺信息
        if (!getLock()) {
            return r;
        }
        //4.1获取锁成功，开启新线程
        //4.2新线程：查库（是常查数据，不会出现不存在的情况），覆盖缓存，释放锁
        EXECUTOR_SERVICE.submit(() -> {
                    try {
                        R apply = dbFallback.apply(id);
                        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(new RedisData(
                                LocalDateTime.now().plusSeconds(unit.toSeconds(time)), apply)));
                        return apply;
                    } finally {
                        //5.返回更新后的缓存信息
                        deleteLock();
                    }
                }
        );
        return null;
    }

    private static boolean getLock() {
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent("lock", "1");
        return BooleanUtil.isTrue(lock);
    }

    private static void deleteLock() {
        stringRedisTemplate.delete("lock");
    }


}
