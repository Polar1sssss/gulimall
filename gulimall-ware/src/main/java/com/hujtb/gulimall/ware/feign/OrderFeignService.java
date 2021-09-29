package com.hujtb.gulimall.ware.feign;

import com.hujtb.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("gulimall-order")
public interface OrderFeignService {

    @GetMapping("/order/order/status/{ordersn}")
    R getOrderStatus(@PathVariable("ordersn") String orderSn);
}
