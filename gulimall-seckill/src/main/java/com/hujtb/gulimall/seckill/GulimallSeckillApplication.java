package com.hujtb.gulimall.seckill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 整合Sentinel
 * 1）导入依赖
 * 2）下载Sentinel控制台jar包
 * 3）设置控制台地址信息
 * 4）在控制台调整参数
 *
 * 每个微服务都导入actuator，同时添加配置项：management.endpoints.web.exposure.include=*
 *
 * 使用Sentinel来保护feign远程调用：熔断
 *  1、调用方熔断保护：feign.sentinel.enabled=true
 *  2、调用方手动指定远程服务降级策略（控制台）
 */
@EnableDiscoveryClient
@SpringBootApplication
public class GulimallSeckillApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallSeckillApplication.class, args);
    }

}
