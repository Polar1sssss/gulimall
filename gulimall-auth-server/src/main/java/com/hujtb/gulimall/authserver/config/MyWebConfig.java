package com.hujtb.gulimall.authserver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MyWebConfig implements WebMvcConfigurer {

    /**
     * 视图映射，添加url跳转逻辑
     * @param registry
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // registry.addViewController("/login.html").setViewName("login");
        // 只能是GET请求才能映射
        registry.addViewController("/reg.html").setViewName("reg");
    }
}
