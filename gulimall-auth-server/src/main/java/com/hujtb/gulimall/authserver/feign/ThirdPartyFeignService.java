package com.hujtb.gulimall.authserver.feign;

import com.hujtb.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("gulimall-third-parth")
public interface ThirdPartyFeignService {

    @GetMapping("/sms/sendCode")
    R sendMessage(@RequestParam("phone") String phone, @RequestParam("code") String code);
}
