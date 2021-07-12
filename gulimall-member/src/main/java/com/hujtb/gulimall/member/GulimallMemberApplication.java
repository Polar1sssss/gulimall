package com.hujtb.gulimall.member;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 远程调用服务
 * 1、引入openfeign
 * 2、编写一个接口，告诉SpringCloud这个接口需要调用远程服务
 * 3、声明接口中的方法时调用远程服务那个请求
 * 4、开启远程调用功能
 */
@EnableFeignClients(basePackages = "com.hujtb.gulimall.member.feign")
@MapperScan("com.hujtb.gulimall.member.dao")
@SpringBootApplication
public class GulimallMemberApplication {
    public static void main(String[] args) {
        SpringApplication.run(GulimallMemberApplication.class, args);
    }
}
