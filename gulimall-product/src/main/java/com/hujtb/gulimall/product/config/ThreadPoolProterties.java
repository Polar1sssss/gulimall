package com.hujtb.gulimall.product.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @ConfigurationProperties：跟配置文件中有相同前缀的属性绑定
 */
@ConfigurationProperties(prefix = "hujtb.thread")
@Component
@Data
public class ThreadPoolProterties {
    private Integer coreSize;
    private Integer maxSize;
    private Integer keepAliveTime;
}
