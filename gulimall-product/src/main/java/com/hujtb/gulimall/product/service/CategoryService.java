package com.hujtb.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hujtb.common.utils.PageUtils;
import com.hujtb.gulimall.product.entity.CategoryEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品三级分类
 *
 * @author hujtb
 * @email hujtb@qq.com
 * @date 2021-07-08 17:55:32
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<CategoryEntity> queryWithTree();
}

