package com.alfo.voucher;

import cn.hutool.json.JSONUtil;
import com.alfo.common.utils.redis.RedisIdWorker;
import com.alfo.voucher.domain.po.VoucherOrder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.concurrent.ListenableFutureCallback;

import javax.annotation.Resource;


@SpringBootTest
@Slf4j
public class test {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    public void stringRedisTemplateTest() {
        RLock lock = redissonClient.getLock("lock:order:" + "1010");

        System.out.println("lock:" + lock);
    }

    @Test
    public void rabbitMqTest() {
        //1.创建CorrelationData
        CorrelationData cd = new CorrelationData();
        //2,给Future添加ConfirmCallback
        cd.getFuture().addCallback(new ListenableFutureCallback<CorrelationData.Confirm>() {
            @Override
            public void onFailure(Throwable ex) {
                //2.1Futurn发生异常时的处理逻辑，基本不触发
                log.error("send message fail:{}", ex.toString());
            }

            @Override
            public void onSuccess(CorrelationData.Confirm result) {
                //2.2Future接收到回执的处理逻辑，参数中的result就是回执内容
                if (result.isAck()) {
                    log.info("消息发送成功, 收到ACK");
                } else {
                    log.info("发送消息失败，收到NACK,reason:{}", result.getReason());
                }
            }
        });
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(1L);
        voucherOrder.setUserId(5L);
        rabbitTemplate.convertAndSend("hmdp.direct", "seckillVoucher", voucherOrder);

    }
}

