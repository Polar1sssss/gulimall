package com.hujtb.gulimall.coupon.service.impl;

import com.hujtb.gulimall.coupon.entity.SeckillSkuRelationEntity;
import com.hujtb.gulimall.coupon.service.SeckillSkuRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hujtb.common.utils.PageUtils;
import com.hujtb.common.utils.Query;

import com.hujtb.gulimall.coupon.dao.SeckillSessionDao;
import com.hujtb.gulimall.coupon.entity.SeckillSessionEntity;
import com.hujtb.gulimall.coupon.service.SeckillSessionService;


@Service("seckillSessionService")
public class SeckillSessionServiceImpl extends ServiceImpl<SeckillSessionDao, SeckillSessionEntity> implements SeckillSessionService {


    @Autowired
    SeckillSkuRelationService relationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SeckillSessionEntity> page = this.page(
                new Query<SeckillSessionEntity>().getPage(params),
                new QueryWrapper<SeckillSessionEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 最近三天的秒杀活动
     *
     * @return
     */
    @Override
    public List<SeckillSessionEntity> getLatest3DaysSession() {
        List<SeckillSessionEntity> list = this.list(new QueryWrapper<SeckillSessionEntity>().between("start_time", startTime(), endTime()));
        if (list != null && list.size() > 0) {
            List<SeckillSessionEntity> collect = list.stream().map(entity -> {
                Long id = entity.getId();
                // 设置该活动关联的商品
                List<SeckillSkuRelationEntity> relationEntities = relationService.list(new QueryWrapper<SeckillSkuRelationEntity>().eq("promotion_session_id", id));
                entity.setRelationSkus(relationEntities);
                return entity;
            }).collect(Collectors.toList());
            return collect;
        }
        return null;
    }

    private String startTime() {
        LocalDate start = LocalDate.now();
        LocalTime min = LocalTime.MIN;
        LocalDateTime startTime = LocalDateTime.of(start, min);
        String format = startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return format;
    }

    private String endTime() {
        LocalDate end = LocalDate.now().plusDays(2);
        LocalTime max = LocalTime.MAX;
        LocalDateTime endTime = LocalDateTime.of(end, max);
        String format = endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return format;
    }

}