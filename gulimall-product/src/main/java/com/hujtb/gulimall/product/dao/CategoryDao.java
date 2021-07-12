package com.hujtb.gulimall.product.dao;

import com.hujtb.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author hujtb
 * @email hujtb@qq.com
 * @date 2021-07-08 17:55:32
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
