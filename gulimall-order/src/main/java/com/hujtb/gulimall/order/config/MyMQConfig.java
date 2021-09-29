package com.hujtb.gulimall.order.config;

import com.hujtb.gulimall.order.entity.OrderEntity;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.impl.AMQImpl;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 创建队列、交换机、绑定关系的configuraiton不会重复创建覆盖
 * 1、第一次使用队列【监听】的时候才会创建
 * 2、Broker没有队列、交换机才会创建
 */
@Configuration
public class MyMQConfig {


    /**
     * 容器中的Queue、Exchange、Binding会自动创建
     * 死信队列
     * 一旦创建好队列，即使属性改变 RabbitMQ中已创建好的队列属性也不会发生变化
     *
     * @return
     */
    @Bean
    public Queue orderDelayQueue() {
        Map<String, Object> arguments = new HashMap<>();
        // 死信交换机
        arguments.put("x-dead-letter-exchange", "order-event-exchange");
        // 死信路由键
        arguments.put("x-dead-letter-routing-key", "order.release.order");
        // 信息存活时长
        arguments.put("x-message-ttl", 60000);
        /*
            Queue(String name,  队列名字
            boolean durable,  是否持久化
            boolean exclusive,  是否排他
            boolean autoDelete, 是否自动删除
            Map<String, Object> arguments) 属性【TTL、死信路由、死信路由键】
         */
        Queue queue = new Queue("order.delay.queue", true, false, false, arguments);
        return queue;
    }

    @Bean
    public Queue orderReleaseOrderQueue() {
        Queue queue = new Queue("order.release.order.queue");
        return queue;
    }

    /**
     * 订单削峰队列，削减秒杀流量
     *
     * @return
     */
    @Bean
    public Queue orderSeckillOrderQueue() {
        Queue queue = new Queue("order.seckill.order.queue", ture, false, false);
    }

    @Bean
    public Exchange orderEventExchange() {
        TopicExchange topicExchange = new TopicExchange("order-event-exchange", true, false);
        return topicExchange;
    }

    @Bean
    public Binding orderCreateOrderBinding() {
        Binding binding = new Binding(
                "order.delay.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.create.order",
                null
        );
        return binding;
    }

    @Bean
    public Binding orderReleaseOrderBinding() {
        Binding binding = new Binding(
                "order.release.order.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.release.order",
                null
        );
        return binding;
    }

    /**
     * 订单释放和库存释放队列进行绑定
     *
     * @return
     */
    @Bean
    public Binding orderReleaseOtherBinding() {
        Binding binding = new Binding(
                "stock.release.stock.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.release.other.#",
                null
        );
        return binding;
    }

    public Binding orderSeckillOrderQueueBinding() {
        return new Binding(
                "order-seckill-order-queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.seckill.order",
                null
        );
    }

}
