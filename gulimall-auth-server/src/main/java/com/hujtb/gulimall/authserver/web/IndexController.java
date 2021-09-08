package com.hujtb.gulimall.authserver.web;

import com.alibaba.fastjson.TypeReference;
import com.hujtb.common.constant.AuthServerConst;
import com.hujtb.common.exception.BizCodeEnum;
import com.hujtb.common.utils.R;
import com.hujtb.common.vo.MemberRespVo;
import com.hujtb.gulimall.authserver.feign.MemberFeignService;
import com.hujtb.gulimall.authserver.feign.ThirdPartyFeignService;
import com.hujtb.gulimall.authserver.vo.UserLoginVo;
import com.hujtb.gulimall.authserver.vo.UserRegistVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class IndexController {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ThirdPartyFeignService thirdPartyFeignService;

    @Autowired
    MemberFeignService memberFeignService;

    /**
     * 发送验证码
     *
     * @param phone
     * @return
     */
    @GetMapping("/sms/sendCode")
    @ResponseBody
    public R sendCode(@RequestParam("phone") String phone) {
        // TODO 接口防刷

        // 从redis获取验证码及其创建的时间
        String redisCode = redisTemplate.opsForValue().get(AuthServerConst.SMS_CODE_CACHE_PREFIX + phone);
        if (StringUtils.isNotEmpty(redisCode)) {
            long l = Long.parseLong(redisCode.split("_")[1]);
            if (System.currentTimeMillis() - l < 60000) {
                // 60s内不能重新刷新
                return R.error(BizCodeEnum.SMS_CODE_EXCEPTION.getCode(), BizCodeEnum.SMS_CODE_EXCEPTION.getMsg());
            }
        }

        String verifyCode = UUID.randomUUID().toString().substring(0, 5);
        // redis缓存验证码，防止同一个手机号60s内刷新页面再次获取验证码
        redisTemplate.opsForValue().set(
                AuthServerConst.SMS_CODE_CACHE_PREFIX + phone,
                verifyCode + "_" + System.currentTimeMillis(),
                10,
                TimeUnit.MINUTES);
        thirdPartyFeignService.sendMessage(phone, verifyCode);
        return R.ok();
    }

    /**
     * 转发：同一个请求，数据会放在请求雨中
     * 重定向：想要携带数据需要使用RedirectAttributes，利用的是session，将数据放到session中，只要跳到下一个页面取出数据后，session
     * 里面的数据就会被删掉，会出现分布式session问题
     * 注册功能
     *
     * @param registVo
     * @param result
     * @param redirectAttributes
     * @return
     */
    @PostMapping("/regist")
    public String regist(@Valid UserRegistVo registVo, BindingResult result, RedirectAttributes redirectAttributes) {
        // 校验未通过
        if (result.hasErrors()) {
            Map<String, String> errors = result.getFieldErrors().stream()
                    .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/reg.html";
        }

        // 校验验证码是否正确
        String phone = registVo.getPhone();
        String verifyCode = registVo.getVerifyCode();
        String codeFromRedis = redisTemplate.opsForValue().get(AuthServerConst.SMS_CODE_CACHE_PREFIX + phone);
        if (StringUtils.isNotEmpty(codeFromRedis)) {
            String code = codeFromRedis.split("_")[0];
            if (verifyCode.equals(code)) {
                redisTemplate.delete(AuthServerConst.SMS_CODE_CACHE_PREFIX + phone);
                // 真正调用远程服务
                R r = memberFeignService.regist(registVo);
                if (r.getCode() == 0) {
                    return "redirect:http://auth.gulimall.com/login.html";
                } else {
                    Map<String, String> errors = new HashMap<String, String>();
                    errors.put("msg", (String) r.get("msg"));
                    // errors.put("msg", r.getData("msg", new TypeReference<String>(){}));
                    redirectAttributes.addFlashAttribute("errors", errors);
                    return "redirect:http://auth.gulimall.com/reg.html";
                }
            } else {
                Map<String, String> errors = new HashMap<>();
                errors.put("code", "验证码错误~");
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:http://auth.gulimall.com/reg.html";
            }
        } else {
            Map<String, String> errors = new HashMap<>();
            errors.put("code", "验证码错误~");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/reg.html";
        }
    }

    @PostMapping("/login")
    public String login(UserLoginVo loginVo, RedirectAttributes attributes, HttpSession httpSession) {
        // 远程调用
        R login = memberFeignService.login(loginVo);
        if (login.getCode() == 0) {
            MemberRespVo data = login.getData(new TypeReference<MemberRespVo>() {
            });
            httpSession.setAttribute(AuthServerConst.LOGIN_USER, data);
            return "redirect:http://gulimall.com";
        } else {
            Map<String, String> errors = new HashMap<>();
            errors.put("msg", login.getData("msg", new TypeReference<String>() {
            }));
            attributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }

    /**
     * 登录页面
     * 如果已经登陆，直接跳转到商城首页
     * @param httpSession
     * @return
     */
    @GetMapping("/login.html")
    public String loginPage(HttpSession httpSession) {
        MemberRespVo attribute = (MemberRespVo) httpSession.getAttribute(AuthServerConst.LOGIN_USER);
        if (attribute != null) {
            // 已经登录
            return "redirect:http://gulimall.com";
        } else {
            return "login";
        }
    }
}
