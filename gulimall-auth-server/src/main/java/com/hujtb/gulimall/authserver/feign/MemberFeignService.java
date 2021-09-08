package com.hujtb.gulimall.authserver.feign;

import com.hujtb.common.utils.R;
import com.hujtb.gulimall.authserver.vo.SocialUser;
import com.hujtb.gulimall.authserver.vo.UserLoginVo;
import com.hujtb.gulimall.authserver.vo.UserRegistVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("gulimall-member")
public interface MemberFeignService {

    @PostMapping("/member/member/regist")
    public R regist(@RequestBody UserRegistVo registVo);

    @PostMapping("/member/member/login")
    public R login(@RequestBody UserLoginVo loginVo);

    @PostMapping("/member/member/oauth2/login")
    public R oauth2Login(@RequestBody SocialUser socialUser);
}
