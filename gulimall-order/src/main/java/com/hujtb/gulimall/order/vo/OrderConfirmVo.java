package com.hujtb.gulimall.order.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class OrderConfirmVo {

    // 收货地址
    @Getter
    @Setter
    List<MemberAddressVo> address;

    // 所有选中的购物项目
    @Getter
    @Setter
    List<OrderItemVo> items;

    // 优惠券信息，积分
    @Getter
    @Setter
    Integer integration;

    BigDecimal total; // 订单总额

    BigDecimal payPrice; // 应付价格

    // 防止重复提交令牌
    @Getter
    @Setter
    String orderToken;

    @Getter
    @Setter
    Map<Long, Boolean> stocks;

    /**
     * 获取商品总件数
     *
     * @return
     */
    public Integer getCount() {
        Integer i = 0;
        if (items != null) {
            for (OrderItemVo item : items) {
                i += item.getCount();
            }
        }
        return i;
    }

    public BigDecimal getTotal() {
        BigDecimal count = new BigDecimal(0);
        if (items != null) {
            for (OrderItemVo item : items) {
                BigDecimal multiply = item.getPrice().multiply(new BigDecimal(item.getCount()));
                count = count.add(multiply);
            }
        }
        return total;
    }

    public BigDecimal getPayPrice() {
        return getTotal();
    }
}
