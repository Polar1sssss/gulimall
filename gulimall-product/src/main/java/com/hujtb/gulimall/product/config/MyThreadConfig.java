package com.hujtb.gulimall.product.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 可变的属性放到配置文件，配置文件与属性配置类绑定，引入属性配置类即可拿到属性值
 * 如果ThreadPoolProterties没有放到容器里，也可以通过@EnableConfigurationProperties把它放进去
 */
// @EnableConfigurationProperties(ThreadPoolProterties.class)
@Configuration
public class MyThreadConfig {

    /**
     * 将ThreadPoolExecutor的实例注入到容器中
     * @param poolProterties
     * @return
     */
    @Bean
    public ThreadPoolExecutor threadPoolExecutor(ThreadPoolProterties poolProterties) {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                poolProterties.getCoreSize(),
                poolProterties.getMaxSize(),
                poolProterties.getKeepAliveTime(),
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(100000),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
        return threadPoolExecutor;
    }
}
