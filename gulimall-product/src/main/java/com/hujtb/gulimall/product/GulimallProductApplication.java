package com.hujtb.gulimall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 1.整合mybatis-plus
 *      1）导入依赖
 *      2）在application.yml中配置数据源相关信息
 *      3）在application.yml中配置mybatis-plus相关信息
 *          使用@MapperScan注解扫描mapper接口欧
 *          告诉Mybatis-plus sql映射文件的位置
 */
@MapperScan("com.hujtb.gulimall.product.dao")
@SpringBootApplication
public class GulimallProductApplication {
    public static void main(String[] args) {
        SpringApplication.run(GulimallProductApplication.class, args);
    }
}
