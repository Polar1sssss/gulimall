package com.hujtb.gulimall.product.dao;

import com.hujtb.gulimall.product.entity.CategoryBrandRelationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 品牌分类关联
 *
 */
@Mapper
public interface CategoryBrandRelationDao extends BaseMapper<CategoryBrandRelationEntity> {

    // @Param：为每一个参数取个名字
    void updateCategory(@Param("catId") Long catId, @Param("name") String name);
}
