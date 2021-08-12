package com.hujtb.gulimall.product.feign;

import com.hujtb.common.to.SkuHasStockTo;
import com.hujtb.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("gulimall-ware")
public interface WareFeignService {

    @PostMapping("/ware/waresku/hasstock")
    R<List<SkuHasStockTo>> getSkusHasStock(@RequestBody List<Long> ids);

}
