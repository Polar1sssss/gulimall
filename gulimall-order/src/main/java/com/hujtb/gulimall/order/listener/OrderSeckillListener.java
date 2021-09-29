package com.hujtb.gulimall.order.listener;

import com.hujtb.common.to.SeckillOrderTo;
import com.hujtb.gulimall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@RabbitListener(queues = "order.seckill.order.queue")
@Service
public class OrderSeckillListener {

    @Autowired
    OrderService orderService;

    @RabbitHandler
    public void listener(SeckillOrderTo orderTo, Channel channel, Message message) throws IOException {
        System.out.println("创建秒杀订单");
        try {
            orderService.createSeckillOrder(orderTo);
            // 手动调用支付宝收单功能

            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }
}