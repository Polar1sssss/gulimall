package com.hujtb.gulimall.product.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 秒杀优惠信息
 */
@Data
public class SeckillSkuVo {

    private Long promotionId;
    private Long promotionSessionId;
    private Long skuId;
    /**
     * 商品秒杀随机码
     */
    private String randomCode;
    private BigDecimal seckillPrice;
    private Integer seckillCount;
    private Integer seckillLimit;
    private Integer seckillSort;
    private Long startTime;
    private Long endTime;
}