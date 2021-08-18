package com.hujtb.gulimall.product;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hujtb.common.utils.PageUtils;
import com.hujtb.gulimall.product.config.MyRedissonConfig;
import com.hujtb.gulimall.product.entity.BrandEntity;
import com.hujtb.gulimall.product.service.BrandService;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@SpringBootTest
class GulimallProductApplicationTests {

    @Autowired
    BrandService brandService;

    @Autowired
    RedissonClient redissonClient;

    @Test
    void contextLoads() {
        System.out.println(redissonClient);
    }

}
