package com.hujtb.gulimall.ware.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

/**
 * 消息中间件配置类
 * 延时任务
 *
 */
@Configuration
public class MyMQConfig {

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Exchange stockEventExchange() {
        Exchange exchange = new TopicExchange("stock-event-exchange", true, false);
        return exchange;
    }

    @Bean
    public Queue stockReleaseStockQueue() {
        return new Queue("stock.release.stock.queue", true, false, false);
    }

    /**
     * 延迟队列
     * @return
     */
    @Bean
    public Queue stockDelayQueue() {
        HashMap<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "stock-event-exchange");
        args.put("x-dead-letter-routing", "stock-locked");
        args.put("x-message-ttl", 120000);
        return new Queue("stock.delay.queue", true, false, false, args);
    }

    @Bean
    public Binding stockLockedBinding() {
        Binding binding = new Binding(
                "stock.delay.queue",
                Binding.DestinationType.QUEUE,
                "stock-event-exchange",
                "stock.locked",
                null
        );
        return binding;
    }

    @Bean
    public Binding stockReleaseBinding() {
        Binding binding = new Binding(
                "stock.release.stock.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "stock.release.#",
                null
        );
        return binding;
    }
}
