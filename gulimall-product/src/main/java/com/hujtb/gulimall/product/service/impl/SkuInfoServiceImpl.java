package com.hujtb.gulimall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.hujtb.common.utils.R;
import com.hujtb.gulimall.product.entity.SkuImagesEntity;
import com.hujtb.gulimall.product.entity.SpuInfoDescEntity;
import com.hujtb.gulimall.product.feign.SeckillFeignService;
import com.hujtb.gulimall.product.service.*;
import com.hujtb.gulimall.product.vo.SeckillInfoVo;
import com.hujtb.gulimall.product.vo.SeckillSkuVo;
import com.hujtb.gulimall.product.vo.SkuItemSaleAttrVo;
import com.hujtb.gulimall.product.vo.SkuItemVo;
import com.hujtb.gulimall.product.vo.SpuItemAttrGroupVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hujtb.common.utils.PageUtils;
import com.hujtb.common.utils.Query;

import com.hujtb.gulimall.product.dao.SkuInfoDao;
import com.hujtb.gulimall.product.entity.SkuInfoEntity;


@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {

    @Autowired
    SkuImagesService skuImagesService;

    @Autowired
    SpuInfoDescService spuInfoDescService;

    @Autowired
    AttrGroupService attrGroupService;

    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    ThreadPoolExecutor pool;

    @Autowired
    SeckillFeignService seckillFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveSkuInfo(SkuInfoEntity skuInfoEntity) {
        this.baseMapper.insert(skuInfoEntity);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SkuInfoEntity> queryWrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        String brandId = (String) params.get("brandId");
        String catalogId = (String) params.get("catalogId");
        String min = (String) params.get("min");
        String max = (String) params.get("max");

        if (StringUtils.isNotEmpty(key)) {
            queryWrapper.and((w) -> {
                w.eq("sku_id", key).or().like("sku_name", key);
            });
        }

        if (StringUtils.isNotEmpty(brandId) && !"0".equalsIgnoreCase(brandId)) {
            queryWrapper.eq("brand_id", brandId);
        }

        if (StringUtils.isNotEmpty(catalogId) && !"0".equalsIgnoreCase(catalogId)) {
            queryWrapper.eq("catalog_id", catalogId);
        }

        if (StringUtils.isNotEmpty(min)) {
            queryWrapper.ge("price", min);
        }
        if (StringUtils.isNotEmpty(max)) {
            try {
                BigDecimal bigDecimal = new BigDecimal(max);
                if (bigDecimal.compareTo(new BigDecimal(0)) == 1) {
                    queryWrapper.le("price", max);
                }
            } catch (Exception e) {

            }
        }

        IPage<SkuInfoEntity> page = this.page(new Query<SkuInfoEntity>().getPage(params), queryWrapper);
        return new PageUtils(page);

    }

    @Override
    public List<SkuInfoEntity> getSkusBySpuId(Long spuId) {
        List<SkuInfoEntity> list = this.list(new QueryWrapper<SkuInfoEntity>().eq("spu_id", spuId));
        return list;
    }

    @Override
    public SkuItemVo item(Long skuId) {
        SkuItemVo itemVo = new SkuItemVo();

        // sku基本信息获取，创建一个新的任务
        CompletableFuture<SkuInfoEntity> infoFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfoEntity info = getById(skuId);
            itemVo.setInfo(info);
            return info;
        }, pool);

        // spu销售属性组合，依赖于第一步的结果
        CompletableFuture<Void> saleAttrFuture = infoFuture.thenAcceptAsync((res) -> {
            List<SkuItemSaleAttrVo> saleAttrVos = skuSaleAttrValueService.getSaleAttrsBySpuId(res.getSpuId());
            itemVo.setSaleAttrs(saleAttrVos);
        }, pool);

        // spu的介绍，依赖于第一步的结果
        CompletableFuture<Void> spuDescFuture = infoFuture.thenAcceptAsync((res) -> {
            SpuInfoDescEntity spuDesc = spuInfoDescService.getById(res.getSpuId());
            itemVo.setSpuDesc(spuDesc);
        }, pool);

        // spu的规格参数信息，查出当前spu对应的所有属性分组信息以及当前分组下所有属性对应的值，依赖于第一步的结果
        CompletableFuture<Void> groupFuture = infoFuture.thenAcceptAsync((res) -> {
            List<SpuItemAttrGroupVo> groupVos = attrGroupService.getAttrGroupWithAttrsBySpuId(res.getSpuId(), res.getCatalogId());
            itemVo.setGroupAttrs(groupVos);
        }, pool);

        // sku图片信息获取，新建一个任务
        CompletableFuture<Void> imagesFuture = CompletableFuture.runAsync(() -> {
            List<SkuImagesEntity> images = skuImagesService.getImagesBySkuId(skuId);
            itemVo.setImages(images);
        }, pool);

        // 查询当前sku是否参与秒杀
        CompletableFuture<Void> seckillFuture = CompletableFuture.runAsync(() -> {
            R r = seckillFeignService.getSkuSeckillInfo(skuId);
            if (r.getCode() == 0) {
                SeckillSkuVo data = r.getData(new TypeReference<SeckillSkuVo>() {
                });
                itemVo.setSeckillSkuVo(data);
            }
        }, pool);

        // 等待所有任务执行完毕，才能返回
        try {
            CompletableFuture.allOf(saleAttrFuture, spuDescFuture, groupFuture, imagesFuture, seckillFuture).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return itemVo;
    }

}