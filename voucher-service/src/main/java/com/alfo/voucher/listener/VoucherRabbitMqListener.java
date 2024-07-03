package com.alfo.voucher.listener;

import com.alfo.voucher.domain.po.VoucherOrder;
import com.alfo.voucher.service.IVoucherOrderService;
import com.alfo.voucher.service.impl.VoucherOrderServiceImpl;
import com.rabbitmq.client.AMQP;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VoucherRabbitMqListener {
    @Autowired
    private VoucherOrderServiceImpl voucherOrderService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "hmdp.seckillVoucher", durable = "true"),
            exchange = @Exchange(name = "hmdp.direct", type = ExchangeTypes.DIRECT),
            key = "seckillVoucher"
    ))
    public void listenerSeckillVoucher(VoucherOrder voucherOrder) throws InterruptedException {
//        System.out.println(voucherOrder);
        voucherOrderService.handleVoucherOrder(voucherOrder);

    }
}
