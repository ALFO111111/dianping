package com.alfo.voucher.config;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
@AllArgsConstructor
@Slf4j
public class RabbitMqConfig {
    private final RabbitTemplate rabbitTemplate;
    @Bean
    public DirectExchange exchange() {
        return ExchangeBuilder.directExchange("hmdp.direct").build();
    }
    @Bean
    public Queue queue() {
        return new Queue("hmdp.seckillVoucher", true);
    }
    @Bean
    public Binding binding(Queue queue, DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("seckillVoucher");
    }

    @PostConstruct
    public void init() {
        rabbitTemplate.setReturnsCallback(new RabbitTemplate.ReturnsCallback() {

            @Override
            public void returnedMessage(ReturnedMessage returnedMessage) {
                log.error("触发return callback");
                log.error("exchange:{}", returnedMessage.getExchange());
                log.error("routingKey:{}", returnedMessage.getRoutingKey());
                log.error("message:{}", returnedMessage.getMessage());
            }
        });
    }
}
