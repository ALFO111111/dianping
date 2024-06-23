package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements Lock {

    private StringRedisTemplate stringRedisTemplate;
    private final String KEY_PREFIX = "lock:";
    private String keyName;
    private final String ID_PREFIX = UUID.randomUUID().toString();
    private String id = ID_PREFIX + ":" + Thread.currentThread().getId();
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate, String keyName) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.keyName = keyName;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        Boolean isLock = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + keyName, id, timeoutSec, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(isLock);
    }

    @Override
    public void unlock() {
        //调用lua脚本：
        stringRedisTemplate.execute(UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + keyName),
                id);
    }

//    @Override
//    public void unlock() {
        //        if (stringRedisTemplate.opsForValue().get(KEY_PREFIX + keyName).equals(id)) {
//            stringRedisTemplate.delete(KEY_PREFIX + keyName);
//        }
//    }
}
