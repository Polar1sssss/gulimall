package com.hujtb.gulimall.order.service.impl;

import com.hujtb.gulimall.order.entity.OrderEntity;
import com.hujtb.gulimall.order.entity.OrderReturnReasonEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hujtb.common.utils.PageUtils;
import com.hujtb.common.utils.Query;

import com.hujtb.gulimall.order.dao.OrderItemDao;
import com.hujtb.gulimall.order.entity.OrderItemEntity;
import com.hujtb.gulimall.order.service.OrderItemService;

@RabbitListener(queues = {"hello-java-queue"})
@Service("orderItemService")
public class OrderItemServiceImpl extends ServiceImpl<OrderItemDao, OrderItemEntity> implements OrderItemService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderItemEntity> page = this.page(
                new Query<OrderItemEntity>().getPage(params),
                new QueryWrapper<OrderItemEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * queues：声明要监听的队列
     * 参数说明 ：
     *  1、org.springframework.amqp.core.Message：原生消息详细信息，头+体
     *  2、T<发送的消息类型>
     *  3、com.rabbitmq.client.Channel：当前传输数据的通道
     *
     *  场景：
     *      1）启动多个订单服务：同一个消息只能被一个客户端收到
     *      2）只有一个消息完全处理完，方法运行结束，才会接收下一个消息
     *
     *  @RabbitListener：可以加在类或方法上
     *  @RabbitHandler：标注在方法上（重载不同的消息）
     */
    @RabbitHandler
    public void receiveMsg(Message msg, OrderReturnReasonEntity content, Channel channel) {
        byte[] body = msg.getBody();
        MessageProperties messageProperties = msg.getMessageProperties();
        System.out.println("接收到消息1..." + content);
        // channel内自增
        long deliveryTag = messageProperties.getDeliveryTag();
        // 手动ack，非批量
        try {
            if (deliveryTag % 2 == 0) {
                channel.basicAck(deliveryTag, false);
                System.out.println("签收了货物..." + deliveryTag);
            } else {
                // 拒绝签收，可以设置是否批量，可以设置拒绝之后是否重新入队
                channel.basicNack(deliveryTag, false, true);
                System.out.println("没签收...");
            }
        } catch (Exception e) {

        }
    }

    @RabbitHandler
    public void receiveMsg2(OrderEntity content) {
        System.out.println("接收到消息2..." + content);
    }

}