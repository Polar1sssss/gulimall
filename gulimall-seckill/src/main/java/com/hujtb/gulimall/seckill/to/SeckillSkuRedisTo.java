package com.hujtb.gulimall.seckill.to;

import com.hujtb.gulimall.seckill.vo.SkuInfoVo;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeckillSkuRedisTo {

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
    private SkuInfoVo skuInfoVo;
    private Long startTime;
    private Long endTime;
}