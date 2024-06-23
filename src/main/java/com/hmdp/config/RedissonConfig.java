package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient() {
        //配置，地址，密码
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.204.129:6379").setPassword("1234");
        //创建Redisson对象
        return Redisson.create(config);
    }
}
