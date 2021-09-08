package com.hujtb.gulimall.order.config;

import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.annotation.PostConstruct;

/**
 * 如果一个类只有一个有参构造器，那么这个构造器的参数会从容器中得到
 * 如果发送的消息是对象，想要将这个对象转换成json，需要此配置类
 */
@Configuration
public class MyRabbitConfig {

    // @Autowired
    RabbitTemplate rabbitTemplate;

//    public MyRabbitConfig(RabbitTemplate rabbitTemplate) {
//        this.rabbitTemplate = rabbitTemplate;
//        initRabbitTemplate();
//    }

    @Primary
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        this.rabbitTemplate = rabbitTemplate;
        rabbitTemplate.setMessageConverter(messageConverter());
        initRabbitTemplate();
        return rabbitTemplate;
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 定制RabbitTemplate
     * @PostConstruct：当前配置类创建完成调用该方法
     * 1、服务器收到消息回调 ConfirmCallback
     * 2、消息正确抵达队列回调 ReturnsCallback
     * 3、
     */
    // @PostConstruct
    public void initRabbitTemplate() {
        // 设置确认回调
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {

            /**
             *
             * @param correlationData 当前消息的唯一关联数据
             * @param ack 消息是否成功收到
             * @param cause 失败的原因
             */
            @Override
            public void confirm(CorrelationData correlationData, boolean ack, String cause) {
                System.out.println("confirm...correlationData-" + correlationData + "cause-" + cause );
            }
        });

        rabbitTemplate.setReturnsCallback(new RabbitTemplate.ReturnsCallback() {
            /**
             * 触发时机：消息没有投递到指定队列触发该方法
             * @param returnedMessage 投递失败的消息的详细内容
             */
            @Override
            public void returnedMessage(ReturnedMessage returnedMessage) {
                System.out.println("FailedMessage[" + returnedMessage + "]");
            }
        });
    }
}
