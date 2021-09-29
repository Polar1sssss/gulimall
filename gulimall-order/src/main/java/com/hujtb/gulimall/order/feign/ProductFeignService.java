package com.hujtb.gulimall.order.feign;

import com.hujtb.gulimall.order.vo.SpuInfoVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("gulimall-product")
public interface ProductFeignService {

    @GetMapping("/product/spuinfo/skuid/{skuId}")
    SpuInfoVo getSpuBySkuId(@PathVariable("skuId") Long skuId);
}
