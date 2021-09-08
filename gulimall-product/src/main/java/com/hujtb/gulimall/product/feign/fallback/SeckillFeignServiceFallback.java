package com.hujtb.gulimall.product.feign.fallback;

import com.hujtb.common.exception.BizCodeEnum;
import com.hujtb.common.utils.R;
import com.hujtb.gulimall.product.feign.SeckillFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SeckillFeignServiceFallback implements SeckillFeignService {
    @Override
    public R getSkuSeckillInfo(Long skuId) {
        log.error("熔断方法调用...getSkuSeckillInfo");
        return R.error(BizCodeEnum.TOOMANY_REQUEST_EXCEPTION.getCode(), BizCodeEnum.TOOMANY_REQUEST_EXCEPTION.getMsg());
    }
}
