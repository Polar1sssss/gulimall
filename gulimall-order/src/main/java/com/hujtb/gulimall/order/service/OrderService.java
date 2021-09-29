package com.hujtb.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hujtb.common.to.SeckillOrderTo;
import com.hujtb.common.utils.PageUtils;
import com.hujtb.common.utils.R;
import com.hujtb.gulimall.order.entity.OrderEntity;
import com.hujtb.gulimall.order.vo.*;

import java.util.Map;

/**
 * 订单
 *
 * @author hujtb
 * @email hujtb@qq.com
 * @date 2021-07-08 18:01:13
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    OrderConfirmVo getConfirm();

    OrderSubmitResponseVo submitOrder(OrderSubmitVo submitVo);

    OrderEntity getOrderStatusBySn(String orderSn);

    void closeOrder(OrderEntity entity);

    PayVo getOrderPay(String orderSn);

    PageUtils queryPageWithItem(Map<String, Object> params);

    String handlePayResult(PayAsyncVo vo);

    void createSeckillOrder(SeckillOrderTo orderTo);
}

