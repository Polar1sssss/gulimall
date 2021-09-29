package com.hujtb.gulimall.seckill.controller;

import com.hujtb.common.utils.R;
import com.hujtb.common.vo.MemberRespVo;
import com.hujtb.gulimall.seckill.service.SeckillService;
import com.hujtb.gulimall.seckill.to.SeckillSkuRedisTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class SeckillController {

    @Autowired
    SeckillService seckillService;

    public static ThreadLocal<MemberRespVo> threadLocal = new ThreadLocal<>();

    /**
     * 返回当前时间可以参加秒杀的商品，即当前时间处于秒杀开始时间和结束时间
     *
     * @return
     */
    @ResponseBody
    @GetMapping("/getCurrentSeckillSkus")
    public R getCurrentSeckillSkus() {
        List<SeckillSkuRedisTo> redisTos = seckillService.getCurrentSeckillSkus();
        return R.ok().setData(redisTos);
    }

    @ResponseBody
    @GetMapping("/sku/seckill/{skuId}")
    public R getSkuSeckillInfo(@PathVariable("skuId") Long skuId) {
        SeckillSkuRedisTo to = seckillService.getSkuSeckillInfo(skuId);
        return R.ok().setData(to);
    }

    @GetMapping("/kill")
    public String kill(@RequestParam("killId") String killId,
                       @RequestParam("key") String code,
                       @RequestParam("num") Integer num, Model model) {
        String orderSn = null;
        try {
            orderSn = seckillService.kill(killId, code, num);
            model.addAttribute("orderSn", orderSn);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "success";
    }
}
