package com.hujtb.gulimall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 1.整合mybatis-plus
 * 1）导入依赖
 * 2）在application.yml中配置数据源相关信息
 * 3）在application.yml中配置mybatis-plus相关信息
 *    使用@MapperScan注解扫描mapper接口欧
 *    告诉Mybatis-plus sql映射文件的位置
 * 2.逻辑删除
 * 配置全局的逻辑删除规则（可忽略）
 * 配置逻辑删除组件（mp3.1.1版本后可忽略此步）
 * 加上逻辑删除注解@TableLogic
 *
 * 3.JSR303数据校验
 * 1）给Bean添加校验注解：javax.validation.constraints，并定义自己的message提示
 * 2）给待校验数据标注@Valid开启校验
 *    效果：校验错误以后给出默认响应
 * 3）在待校验bean后加入BindingResult，就可以获取校验结果
 * 4）分组校验
 *    给校验注解添加group属性，指定那些情况需要校验
 *    @NotNull(message = "修改必须指定品牌id", groups = {UpdateGroup.class})
 *    @Null(message = "新增时不能指定id", groups = {AddGroup.class})
 *    Controller方法上添加@Validated({AddGroup.class})
 * 5）自定义校验
 *    编写自定义校验注解 ListValue
 *    编写自定义校验器 ListValueConstraintValue
 *    关联注解和校验器 @Constraint(validatedBy = { ListValueConstraintValue.class [可以指定多个不同类型校验器]})
 * 4.统一异常处理
 * 5.模板引擎
 * 1）引入thymeleaf的starter，关闭缓存，能够实时更新数据
 * 2）静态资源放在static文件夹下，就可以按照路径正常访问
 * 3）页面放在templates文件夹下，直接访问，SpringBoot默认访问index
 */

@MapperScan("com.hujtb.gulimall.product.dao")
@EnableFeignClients(basePackages = "com.hujtb.gulimall.product.feign")
@EnableDiscoveryClient
@SpringBootApplication
public class GulimallProductApplication {
    public static void main(String[] args) {
        SpringApplication.run(GulimallProductApplication.class, args);
    }
}
