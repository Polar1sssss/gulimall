package com.hujtb.gulimall.ware.feign;

import com.hujtb.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("gulimall-product")
// @FeignClient("gulimall-api")
public interface ProductFeignService {
    @RequestMapping("/product/skuinfo/info/{skuId}")
    // @RequestMapping("/api/product/skuinfo/info/{skuId}")
    public R info(@PathVariable("skuId") Long skuId);
}
