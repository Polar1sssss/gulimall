package com.hujtb.gulimall.cart.interceptor;

import com.hujtb.common.constant.AuthServerConst;
import com.hujtb.common.vo.MemberRespVo;
import com.hujtb.gulimall.cart.constant.CartConst;
import com.hujtb.gulimall.cart.vo.UserInfoTo;
import org.apache.catalina.User;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

public class CartInterceptor implements HandlerInterceptor {

    // 同一个线程之间共享数据
    public static ThreadLocal<UserInfoTo> threadLocal = new ThreadLocal<>();

    /**
     * 目标方法执行前
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();
        UserInfoTo userInfoTo = new UserInfoTo();
        // 获取登录后返回的用户，登录成功会把user放到session中
        MemberRespVo respVo = (MemberRespVo) session.getAttribute(AuthServerConst.LOGIN_USER);

        if (respVo != null) {
            // 如果已经登录，给userInfoTo设置属性
            userInfoTo.setUserId(respVo.getId());
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                String name = cookie.getName();
                // 如果cookie中已经有user-key，直接设置
                if (name.equals(CartConst.TEMP_USER_COOKIE_NAME)) {
                    userInfoTo.setUserKey(cookie.getValue());
                }
            }
        }
        // 如果没有从userInfoTo中取到user-key，证明cookie中没有user-key，生成一个新的设置进去
        if (StringUtils.isEmpty(userInfoTo.getUserKey())) {
            String uuid = UUID.randomUUID().toString();
            userInfoTo.setUserKey(uuid);
            userInfoTo.setTmpUser(true);
        }
        threadLocal.set(userInfoTo);
        return true;
    }

    /**
     * 业务执行之后，分配临时用户，让浏览器保存
     *
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

        UserInfoTo userInfoTo = threadLocal.get();
        // 如果是第一次访问，需要给user-key设置作用域和过期时间
        if (userInfoTo.isTmpUser()) {
            Cookie cookie = new Cookie(CartConst.TEMP_USER_COOKIE_NAME, userInfoTo.getUserKey());
            cookie.setDomain("gulimall.com");
            cookie.setMaxAge(CartConst.TEMP_USER_COOKIE_TIMEOUT);
            response.addCookie(cookie);
        }
    }
}
