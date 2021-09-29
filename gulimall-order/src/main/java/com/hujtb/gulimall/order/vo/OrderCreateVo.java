package com.hujtb.gulimall.order.vo;

import com.hujtb.gulimall.order.entity.OrderEntity;
import com.hujtb.gulimall.order.entity.OrderItemEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderCreateVo {

    // 订单
    private OrderEntity order;
    // 订单商品
    private List<OrderItemEntity> orderItems;
    private BigDecimal payPrice;
    private BigDecimal fare;

}
