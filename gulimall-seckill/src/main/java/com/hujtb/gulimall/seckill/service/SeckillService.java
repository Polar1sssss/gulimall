package com.hujtb.gulimall.seckill.service;

import com.hujtb.gulimall.seckill.to.SeckillSkuRedisTo;

import java.util.List;

public interface SeckillService {

    void uploadSeckillSkuLatest3Days();

    List<SeckillSkuRedisTo> getCurrentSeckillSkus();

    SeckillSkuRedisTo getSkuSeckillInfo(Long skuId);

    String kill(String killId, String code, Integer num);
}
