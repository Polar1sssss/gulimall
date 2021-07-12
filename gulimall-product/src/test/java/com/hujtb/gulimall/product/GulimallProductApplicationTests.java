package com.hujtb.gulimall.product;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hujtb.common.utils.PageUtils;
import com.hujtb.gulimall.product.entity.BrandEntity;
import com.hujtb.gulimall.product.service.BrandService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@SpringBootTest
class GulimallProductApplicationTests {

    @Autowired
    BrandService brandService;

    @Test
    void contextLoads() {
//        BrandEntity brand = new BrandEntity();
//        brand.setBrandId(1L);
//        brand.setDescript("华为");
//        brand.setName("huawei");
//        brandService.save(brand);
//        System.out.println("保存成功");
//        brandService.updateById(brand);
        List<BrandEntity> list = brandService.list(new QueryWrapper<BrandEntity>().eq("brand_id", 1L));
        for (BrandEntity item : list) {
            System.out.println(item.getBrandId() + item.getName() + item.getDescript());
        }
    }

}
