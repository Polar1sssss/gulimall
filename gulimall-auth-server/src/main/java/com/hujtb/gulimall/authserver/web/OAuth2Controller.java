package com.hujtb.gulimall.authserver.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.hujtb.common.constant.AuthServerConst;
import com.hujtb.common.utils.HttpUtils;
import com.hujtb.common.utils.R;
import com.hujtb.gulimall.authserver.feign.MemberFeignService;
import com.hujtb.common.vo.MemberRespVo;
import com.hujtb.gulimall.authserver.vo.SocialUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.HashMap;

/**
 * 处理社交登录请求
 */
@Slf4j
@Controller
public class OAuth2Controller {

    @Autowired
    MemberFeignService memberFeignService;

    /**
     * 微博授权成功，处理回调请求
     * @param code
     * @return
     */
    @GetMapping("/oauth2.0/weibo/success")
    public String weibo(@RequestParam("code") String code, HttpSession httpSession) {

        // 根据code换取access_token
        HashMap<String, String> map = new HashMap<>();
        map.put("client_id", "xxx");
        map.put("client_secret", "xxx");
        map.put("grant_type", "authorization_code");
        // 登录成功后重定向的地址
        map.put("redirect_uri", "http://gulimall.com/oauth2.0/weibo/success");
        // 微博授权成功后返回的code，用它换取access_token
        map.put("code", code);
        try {
            // 根据code换取token
            HttpResponse response = HttpUtils.doPost("api.weibo.com", "/oauth2/access_token", "post", null, null, map);
            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                String json = EntityUtils.toString(entity);
                SocialUser socialUser = JSON.parseObject(json, SocialUser.class);
                // 远程调用社交用户登录方法
                R r = memberFeignService.oauth2Login(socialUser);
                if (r.getCode() == 0) {
                    MemberRespVo data = r.getData(new TypeReference<MemberRespVo>(){});
                    log.info("登录成功，用户信息:{}", data);
                    // TODO 1、默认发的令牌：session=abcd，作用域：当前域（解决子域session共享，应当提升作用域范围）
                    // TODO 2、使用JSON进行序列化
                    httpSession.setAttribute(AuthServerConst.LOGIN_USER, data);

                    return "redirect:http://gulimall.com";
                } else {
                    return "redirect:http://auth.gulimall.com/login.html";
                }
            } else {
                return "redirect:http://auth.gulimall.com/login.html";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 登录成功就跳回首页
        return "redirect:http://gulimall.com";
    }
}
