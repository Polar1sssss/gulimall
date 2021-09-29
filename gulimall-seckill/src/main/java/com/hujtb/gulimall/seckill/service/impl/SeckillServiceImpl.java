package com.hujtb.gulimall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.hujtb.common.to.SeckillOrderTo;
import com.hujtb.common.utils.R;
import com.hujtb.common.vo.MemberRespVo;
import com.hujtb.gulimall.seckill.feign.CouponFeignService;
import com.hujtb.gulimall.seckill.feign.ProductFeignService;
import com.hujtb.gulimall.seckill.interceptor.LoginUserInterceptor;
import com.hujtb.gulimall.seckill.service.SeckillService;
import com.hujtb.gulimall.seckill.vo.SeckillSessionsWithSkus;
import com.hujtb.gulimall.seckill.vo.SeckillSkuVo;
import com.hujtb.gulimall.seckill.vo.SkuInfoVo;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.hujtb.gulimall.seckill.to.SeckillSkuRedisTo;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    CouponFeignService couponFeignService;
    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    RedissonClient redissonClient;

    @Autowired
    RabbitTemplate rabbitTemplate;

    private final String SESSIONS_CACHE_PREFIX = "seckill:sessions:";
    private final String SKUS_CACHE_PREFIX = "seckill:skus";

    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:"; // 后面+商品随机码

    @Override
    public void uploadSeckillSkuLatest3Days() {
        // 扫描需要参与秒杀的活动
        R r = couponFeignService.getLatest3DaysSession();
        if (r.getCode() == 0) {
            List<SeckillSessionsWithSkus> data = r.getData(new TypeReference<List<SeckillSessionsWithSkus>>() {
            });
            // 上架商品，将活动商品缓存到redis
            // 1、缓存活动信息
            saveSessionInfos(data);
            // 2、缓存活动相关商品信息
            saveSessionSkuInfos(data);
        }
    }

    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {
        // 确定当前是哪个秒杀场次
        Long time = new Date().getTime();
        Set<String> keys = redisTemplate.keys(SESSIONS_CACHE_PREFIX + "*");
        for (String key : keys) {
            String replace = key.replace(SESSIONS_CACHE_PREFIX, "");
            String[] strings = replace.split("_");
            Long start = Long.parseLong(strings[0]);
            Long end = Long.parseLong(strings[1]);
            if (time >= start && time <= end) {
                // 获取这个秒杀场次需要的所有商品信息
                List<String> range = redisTemplate.opsForList().range(key, -100, 100);
                BoundHashOperations<String, String, String> ops = redisTemplate.boundHashOps(SKUS_CACHE_PREFIX);
                List<String> list = ops.multiGet(range);
                if (list != null && list.size() > 0) {
                    List<SeckillSkuRedisTo> collect = list.stream().map(ele -> {
                        SeckillSkuRedisTo redisTo = JSON.parseObject(ele, SeckillSkuRedisTo.class);
                        // redisTo.setRandomCode(null); // 当前秒杀开始就需要随机码
                        return redisTo;
                    }).collect(Collectors.toList());
                    return collect;
                }
                break;
            }
        }
        return null;
    }

    @Override
    public SeckillSkuRedisTo getSkuSeckillInfo(Long skuId) {
        BoundHashOperations<String, String, String> ops = redisTemplate.boundHashOps(SKUS_CACHE_PREFIX);
        Set<String> keys = ops.keys();
        if (keys != null && keys.size() > 0) {
            String reg = "\\d" + skuId;
            for (String key : keys) {
                // 从秒杀商品中找到了该商品
                if (Pattern.matches(reg, key)) {
                    String json = ops.get(key);
                    SeckillSkuRedisTo to = JSON.parseObject(json, SeckillSkuRedisTo.class);
                    Long current = new Date().getTime();
                    if (!(current >= to.getStartTime() && current <= to.getEndTime())) {
                        // 不在秒杀时间内不需要设置随机码
                        to.setRandomCode(null);
                    }
                    return to;
                }
            }
        }
        return null;
    }

    // TODO 上架秒杀商品时，每一个数据都有过期时间
    // TODO 秒杀的后续流程，简化了收货地址运费计算
    @Override
    public String kill(String killId, String code, Integer num) {
        MemberRespVo member = LoginUserInterceptor.threadLocal.get();
        // 1、获取当前秒杀商品的详细信息
        BoundHashOperations<String, String, String> ops = redisTemplate.boundHashOps(SKUS_CACHE_PREFIX);
        String json = ops.get(killId);
        if (json != null) {
            SeckillSkuRedisTo to = JSON.parseObject(json, SeckillSkuRedisTo.class);
            // 校验时间合法性
            Long current = new Date().getTime();
            Long startTime = to.getStartTime();
            Long endTime = to.getEndTime();
            Long ttl = endTime - startTime;
            if (current >= startTime && current <= endTime) {
                // 校验随机码和商品id
                String randomCode = to.getRandomCode();
                String proId = to.getPromotionSessionId() + "_" + to.getSkuId();
                if (randomCode.equals(code) && killId.equals(proId)) {
                    // 验证购物数量是否合理
                    if (num <= to.getSeckillLimit()) {
                        // 验证这个人是否已经购买过，幂等性；只要秒杀成功就去站位，userId+sessionId+skuId  SETNX
                        String redisKey = member.getId() + "_" + proId;
                        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(redisKey, num.toString(), ttl, TimeUnit.MILLISECONDS);
                        if (aBoolean) {
                            // 占位成功，说明从来没买过
                            RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + randomCode);
                            Boolean b = semaphore.tryAcquire(num);
                            // 秒杀成功
                            if (b) {
                                // 快速下单，发送MQ消息
                                String timeId = IdWorker.getTimeId();
                                SeckillOrderTo orderTo = new SeckillOrderTo();
                                orderTo.setOrderSn(timeId);
                                orderTo.setMemberId(member.getId());
                                orderTo.setNum(num);
                                orderTo.setPromotionSessionId(to.getPromotionSessionId());
                                orderTo.setSkuId(to.getSkuId());
                                orderTo.setSeckillPrice(to.getSeckillPrice());
                                rabbitTemplate.convertAndSend(
                                        "order-event-exchange",
                                        "order.seckill.order", orderTo);
                                return timeId;
                            }
                            return null;
                        } else {
                            return null;
                        }
                    }
                }
            }
        }
        return null;
    }

    private void saveSessionInfos(List<SeckillSessionsWithSkus> sessions) {
        sessions.stream().forEach(session -> {
            Long startTime = session.getStartTime().getTime();
            Long endTime = session.getEndTime().getTime();
            String key = SESSIONS_CACHE_PREFIX + startTime + "_" + endTime;
            if (!redisTemplate.hasKey(key)) {
                // 场次_商品ID作为key进行保存
                List<String> vals = session.getRelationSkus().stream()
                        .map(item -> item.getId() + "_" + item.getSkuId().toString())
                        .collect(Collectors.toList());
                redisTemplate.opsForList().leftPushAll(key, vals);
            }
        });
    }

    private void saveSessionSkuInfos(List<SeckillSessionsWithSkus> sessions) {
        sessions.stream().forEach(session -> {
            BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(SKUS_CACHE_PREFIX);
            for (SeckillSkuVo seckillSkuVo : session.getRelationSkus()) {
                String code = UUID.randomUUID().toString().replaceAll("-", "");
                if (!redisTemplate.hasKey(seckillSkuVo.getPromotionSessionId() + "_" + seckillSkuVo.getSkuId().toString())) {
                    // 缓存商品
                    SeckillSkuRedisTo redisTo = new SeckillSkuRedisTo();
                    // 1、sku基本数据
                    R r = productFeignService.getSkuInfo(seckillSkuVo.getSkuId());
                    if (r.getCode() == 0) {
                        SkuInfoVo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                        });
                        redisTo.setSkuInfoVo(skuInfo);
                    }
                    // 2、sku秒杀信息
                    BeanUtils.copyProperties(seckillSkuVo, redisTo);
                    // 3、设置当前商品秒杀的开始时间和结束时间
                    redisTo.setStartTime(session.getStartTime().getTime());
                    redisTo.setEndTime(session.getEndTime().getTime());
                    // 4、随机码
                    redisTo.setRandomCode(code);
                    String s = JSON.toJSONString(redisTo);
                    ops.put(seckillSkuVo.getPromotionSessionId() + "_" + seckillSkuVo.getSkuId().toString(), s);

                    // 引入分布式信号量  限流
                    // 如果当前场次的商品库存信息已经上架，就无需再上架，所以库存上架是跟skuId相关的
                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + code);
                    // 商品可以秒杀的数量作为信号量
                    semaphore.trySetPermits(seckillSkuVo.getSeckillCount());
                }
            }
        });
    }
}
