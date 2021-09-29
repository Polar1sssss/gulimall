package com.hujtb.gulimall.coupon.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


import com.hujtb.gulimall.coupon.service.SeckillSkuRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.hujtb.gulimall.coupon.entity.SeckillSessionEntity;
import com.hujtb.gulimall.coupon.service.SeckillSessionService;
import com.hujtb.common.utils.PageUtils;
import com.hujtb.common.utils.R;


/**
 * 秒杀活动场次
 *
 * @author hujtb
 * @email hujtb@qq.com
 * @date 2021-07-08 18:03:21
 */
@RestController
@RequestMapping("coupon/seckillsession")
public class SeckillSessionController {

    @Autowired
    private SeckillSessionService seckillSessionService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = seckillSessionService.queryPage(params);
        return R.ok().put("page", page);
    }

    @GetMapping("/latest3DaysSession")
    public R getLatest3DaysSession() {
        List<SeckillSessionEntity> entities = seckillSessionService.getLatest3DaysSession();
        return R.ok().setData(entities);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id) {
        SeckillSessionEntity seckillSession = seckillSessionService.getById(id);
        return R.ok().put("seckillSession", seckillSession);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody SeckillSessionEntity seckillSession) {
        seckillSessionService.save(seckillSession);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody SeckillSessionEntity seckillSession) {
        seckillSessionService.updateById(seckillSession);
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids) {
        seckillSessionService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
