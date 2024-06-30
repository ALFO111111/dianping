package com.alfo.voucher;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.alfo.voucher.mapper")
@SpringBootApplication
public class VoucherApplication {
    public static void main(String[] args) {
        SpringApplication.run(VoucherApplication.class, args);
    }
}