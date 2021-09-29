package com.hujtb.gulimall.seckill;

import org.redisson.spring.session.config.EnableRedissonHttpSession;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

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
 *
 *
 *  高并发系统关注的问题：
 *     1、服务单一职责原则+独立部署
 *     2、秒杀链接加密
 *     3、库存预热 + 快速扣减
 *     4、动静分离
 *     5、恶意请求
 *     6、流量错峰
 *     7、限流&熔断&降级
 *     8、队列削峰
 */
@EnableRedisHttpSession
@EnableDiscoveryClient
@EnableFeignClients
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class GulimallSeckillApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallSeckillApplication.class, args);
    }

}