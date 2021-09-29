package com.hujtb.gulimall.cart.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车
 * 需要计算的属性必须重写get方法，保证每次获取属性都要计算
 */
public class CartVo {

    private List<CartItemVo> items;

    private Integer countNum; // 商品件数

    private Integer countType; // 商品类型数

    private BigDecimal totalAmount; // 商品总价

    private BigDecimal reduce = new BigDecimal(0.00);

    public List<CartItemVo> getItems() {
        return items;
    }

    public void setItems(List<CartItemVo> items) {
        this.items = items;
    }

    public Integer getCountNum() {
        int countNum = 0;
        if (items != null && items.size() > 0) {
            for (CartItemVo itemVo : items) {
                countNum += itemVo.getCount();
            }
        }
        return countNum;
    }

    public Integer getCountType() {
        return items.size();
    }

    public BigDecimal getTotalAmount() {
        BigDecimal amount = new BigDecimal(0);
        if (items != null && items.size() > 0) {
            for (CartItemVo itemVo : items) {
                if (itemVo.getChecked()) {
                    amount = amount.add(itemVo.getTotalPrice());
                }
            }
        }
        return amount.subtract(this.getReduce());
    }

    public BigDecimal getReduce() {
        return reduce;
    }

    public void setReduce(BigDecimal reduce) {
        this.reduce = reduce;
    }
}
