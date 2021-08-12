package com.hujtb.gulimall.product.feign;

import com.hujtb.common.to.SkuReductionTo;
import com.hujtb.common.to.SpuBoundsTo;
import com.hujtb.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("gulimall-coupon")
public interface CouponFeignService {

    /**
     * CouponFeignService.saveSpuBounds(spuBoundsTo)做了哪些事情：
     *  1）@RequestBody注解将SpuBoundsTo对象转为json
     *  2）找到gulimall-coupon服务，向其发送/coupon/spubounds/save请求，将上一步转的json放在请求体位置
     *  3）被调用服务接收到请求，请求体里有json数据。
     *     (@RequestBody SpuBoundsEntity spuBounds)：将请求体中的json转为SpuBoundsEntity对象
     * 总结：只要json数据模型是兼容的，双方服务无需使用同一个to
     * @param spuBoundsTo
     * @return
     */
    @PostMapping("/coupon/spubounds/save")
    R saveSpuBounds(@RequestBody SpuBoundsTo spuBoundsTo);

    @PostMapping("/coupon/skufullreduction/saveinfo")
    R saveSkuReduction(@RequestBody SkuReductionTo skuReductionTo);
}
