package com.alfo.common.config;

import com.alfo.common.utils.redis.CacheClient;
import com.alfo.common.utils.redis.RedisIdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {
//    @Autowired
//    private static StringRedisTemplate stringRedisTemplate;

    @Bean
    public RedisIdWorker redisIdWorker() {
        return new RedisIdWorker();
    }

    @Bean
    public CacheClient cacheClient(StringRedisTemplate stringRedisTemplate) {
        return new CacheClient(stringRedisTemplate);
    }

}
