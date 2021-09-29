package com.hujtb.gulimall.ware.listener;

import com.hujtb.common.to.StockLockedTo;
import com.hujtb.gulimall.ware.service.WareSkuService;
import com.hujtb.common.to.OrderTo;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@RabbitListener(queues = "stock.release.stock.queue")
@Service
public class StockReleaseListener {

    @Autowired
    WareSkuService wareSkuService;

    /**
     * 库存自动解锁
     * 解锁库存失败，一定要告诉mq解锁失败，启动手动ack机制
     *
     * @param to
     * @param message
     */
    @RabbitHandler
    public void handleLockedStockRelease(StockLockedTo to, Message message, Channel channel) throws IOException {
        System.out.println("收到解锁库存的消息");
        try {
            wareSkuService.unLockStock(to);
            // 消息消费成功，手动确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            // 出现异常，应当让消息重新入队，等待其他人消费
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }

    @RabbitHandler
    public void handleOrderClosed(OrderTo orderTo, Message message, Channel channel) throws IOException {
        System.out.println("订单自动取消，通知解锁库存");
        try {
            wareSkuService.unLockStock(orderTo);
            // 消息消费成功，手动确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            // 出现异常，应当让消息重新入队，等待其他人消费
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }
}
