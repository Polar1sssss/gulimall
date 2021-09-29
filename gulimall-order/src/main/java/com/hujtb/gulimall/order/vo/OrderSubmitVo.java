package com.hujtb.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 封装订单提交的数据
 */
@Data
public class OrderSubmitVo {

    // 收货地址id
    private Long addrId;
    // 支付方式
    private String payType;
    // 无需提交需要购买的商品，重新去购物车获取即可，这样能够实时计算最终价格

    // 防重令牌
    private String orderToken;
    // 验价
    private BigDecimal payPrice;
    // 用户相关信息只需要去session中取

    // 备注
    private String note;
}
