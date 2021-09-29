package com.hujtb.gulimall.seckill.feign;

import com.hujtb.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("gulimall-coupon")
public interface CouponFeignService {

    @GetMapping("/coupon/seckillsession/latest3DaysSession")
    R getLatest3DaysSession();

}
