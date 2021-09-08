package com.hujtb.gulimall.order;

import com.hujtb.gulimall.order.entity.OrderEntity;
import com.hujtb.gulimall.order.entity.OrderReturnReasonEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.UUID;

@Slf4j
@SpringBootTest
class GulimallOrderApplicationTests {

    // 管理组件
    @Autowired
    AmqpAdmin amqpAdmin;

    // 消息发送处理组件
    @Autowired
    RabbitTemplate rabbitTemplate;

    void contextLoads() {
    }

    @Test
    void createObject() {
        // 创建交换机
        DirectExchange directExchange = new DirectExchange("hello-java-exchange", true, false);
        amqpAdmin.declareExchange(directExchange);
        log.info("Exchange[{}]创建完成", "hello-java-exchange");

        // 创建队列
        Queue queue1 = new Queue("hello-java-queue", true, false, false);
        amqpAdmin.declareQueue(queue1);
        log.info("Queue[{}]创建完成", "hello-java-queue");

        /**
         * 创建绑定关系
         * String destination：【目的地，即要绑定的队列名字】
         * Binding.DestinationType destinationType：【目的地类型】
         * String exchange：【绑定的交换机名字】
         * String routingKey：【路由键】
         * @Nullable Map<String, Object> arguments：【参数】
         */
        Binding binding = new Binding(
                "hello-java-queue",
                Binding.DestinationType.QUEUE,
                "hello-java-exchange",
                "hello.java", null);
        amqpAdmin.declareBinding(binding);
        log.info("Binding[{}]创建完成", "hello.java");
    }

    @Test
    void sendMessage() {
        String msg = "hahaha";
        // 如果发送的消息是个对象，会使用序列化机制将对象写出去，所以要求对象实现Serializable接口

        for (int i = 0; i < 10; i++) {
            if (i % 2 == 0) {
                OrderReturnReasonEntity entity = new OrderReturnReasonEntity();
                entity.setId(1L);
                entity.setCreateTime(new Date());
                entity.setName("haha" + i);
                rabbitTemplate.convertAndSend("hello-java-exchange",
                        "hello.java",
                        entity,
                        new CorrelationData(UUID.randomUUID().toString()));
            } else {
                OrderEntity entity = new OrderEntity();
                entity.setId(Long.valueOf(UUID.randomUUID().toString()));
                rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", entity);
            }
        }
    }

}
