package com.hujtb.gulimall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hujtb.common.utils.PageUtils;
import com.hujtb.common.utils.Query;
import com.hujtb.gulimall.product.dao.BrandDao;
import com.hujtb.gulimall.product.dao.CategoryBrandRelationDao;
import com.hujtb.gulimall.product.dao.CategoryDao;
import com.hujtb.gulimall.product.entity.BrandEntity;
import com.hujtb.gulimall.product.entity.CategoryBrandRelationEntity;
import com.hujtb.gulimall.product.entity.CategoryEntity;
import com.hujtb.gulimall.product.service.BrandService;
import com.hujtb.gulimall.product.service.CategoryBrandRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service("categoryBrandRelationService")
public class CategoryBrandRelationServiceImpl extends ServiceImpl<CategoryBrandRelationDao, CategoryBrandRelationEntity> implements CategoryBrandRelationService {

    @Resource
    private BrandDao brandDao;

    @Resource
    private CategoryDao categoryDao;

    @Autowired
    private CategoryBrandRelationDao relationDao;

    @Autowired
    private BrandService brandService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryBrandRelationEntity> page = this.page(
                new Query<CategoryBrandRelationEntity>().getPage(params),
                new QueryWrapper<CategoryBrandRelationEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveDetail(CategoryBrandRelationEntity categoryBrandRelation) {
        Long brandId = categoryBrandRelation.getBrandId();
        Long catalogId = categoryBrandRelation.getCatalogId();

        //1???????????????????????????
        BrandEntity brandEntity = brandDao.selectById(brandId);
        //2???????????????????????????
        CategoryEntity categoryEntity = categoryDao.selectById(catalogId);

        //??????????????????categoryBrandRelation???
        categoryBrandRelation.setBrandName(brandEntity.getName());
        categoryBrandRelation.setCatalogName(categoryEntity.getName());

        // ?????????????????????
        this.baseMapper.insert(categoryBrandRelation);
    }

    /**
     * ?????????????????????????????????
     *  1?????????UpdateWrapper
     *  2?????????baseMapper????????????xml??????
     * @param brandId
     * @param name
     */
    @Override
    public void updateBrand(Long brandId, String name) {
        CategoryBrandRelationEntity relationEntity = new CategoryBrandRelationEntity();
        relationEntity.setBrandId(brandId);
        relationEntity.setBrandName(name);
        this.update(relationEntity, new UpdateWrapper<CategoryBrandRelationEntity>().eq("brand_id", brandId));
    }

    @Override
    public void updateCategory(Long catId, String name) {
        this.baseMapper.updateCategory(catId, name);
    }

    @Override
    public List<BrandEntity> getBrandsByCatId(Long catId) {

        List<CategoryBrandRelationEntity> catalogId = relationDao.selectList(new QueryWrapper<CategoryBrandRelationEntity>().eq("catalog_id", catId));

        List<BrandEntity> collect = catalogId.stream().map(item -> {
            Long brandId = item.getBrandId();
            //?????????????????????
            BrandEntity byId = brandService.getById(brandId);
            return byId;
        }).collect(Collectors.toList());

        return collect;
    }

}
