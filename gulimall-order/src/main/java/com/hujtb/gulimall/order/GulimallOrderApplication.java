package com.hujtb.gulimall.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 使用RabbitMQ
 * 1、引入amqp场景：RabbitAutoConfiguration会自动生效
 * 2、给容器中自动配置了RabbitTemplate、AmqpAdmin、CachingConnnectionFactory
 * 3、@EnableRabbit
 */
@EnableRabbit
@MapperScan("com.hujtb.gulimall.order.dao")
@SpringBootApplication
public class GulimallOrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(GulimallOrderApplication.class, args);
    }
}
