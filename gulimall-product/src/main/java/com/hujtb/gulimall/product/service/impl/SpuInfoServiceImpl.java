package com.hujtb.gulimall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.hujtb.common.constant.ProductConst;
import com.hujtb.common.to.SkuHasStockTo;
import com.hujtb.common.to.SkuReductionTo;
import com.hujtb.common.to.SpuBoundsTo;
import com.hujtb.common.to.es.SkuEsModel;
import com.hujtb.common.utils.R;
import com.hujtb.gulimall.product.entity.*;
import com.hujtb.gulimall.product.feign.CouponFeignService;
import com.hujtb.gulimall.product.feign.SearchFeignService;
import com.hujtb.gulimall.product.feign.WareFeignService;
import com.hujtb.gulimall.product.service.*;
import com.hujtb.gulimall.product.vo.*;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hujtb.common.utils.PageUtils;
import com.hujtb.common.utils.Query;

import com.hujtb.gulimall.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    SpuInfoDescService spuInfoDescService;

    @Autowired
    SpuImagesService spuImagesService;

    @Autowired
    AttrService attrService;

    @Autowired
    ProductAttrValueService valueService;

    @Autowired
    SkuInfoService skuInfoService;

    @Autowired
    SkuImagesService skuImagesService;

    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    BrandService brandService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    WareFeignService wareFeignService;

    @Autowired
    SearchFeignService searchFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }


    @GlobalTransactional
    @Override
    public void saveSpuInfo(SpuSaveVo vo) {
        // 1????????????????????? pms_spu_info
        SpuInfoEntity spuInfoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(vo, spuInfoEntity);
        spuInfoEntity.setCreateTime(new Date());
        spuInfoEntity.setUpdateTime(new Date());
        this.saveBaseSpuInfo(spuInfoEntity);

        // 2?????????spu???????????? pms_spu_info_desc
        List<String> decript = vo.getDecript();
        SpuInfoDescEntity spuInfoDescEntity = new SpuInfoDescEntity();
        Long spuId = spuInfoDescEntity.getSpuId();
        spuInfoDescEntity.setSpuId(spuId);
        spuInfoDescEntity.setDecript(String.join(",", decript));
        spuInfoDescService.saveSpuInfoDesc(spuInfoDescEntity);

        // 3?????????????????? pms_spu_images
        List<String> images = vo.getImages();
        spuImagesService.saveSpuImages(spuInfoEntity.getId(), images);

        // 4?????????spu???????????? pms_product_attr_value
        List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
        List<ProductAttrValueEntity> collect = baseAttrs.stream().map(attr -> {
            ProductAttrValueEntity valueEntity = new ProductAttrValueEntity();
            valueEntity.setAttrId(attr.getAttrId());
            AttrEntity byId = attrService.getById(attr.getAttrId());
            valueEntity.setAttrName(byId.getAttrName());
            valueEntity.setAttrValue(attr.getAttrValues());
            valueEntity.setQuickShow(attr.getShowDesc());
            valueEntity.setSpuId(spuInfoEntity.getId());
            return valueEntity;
        }).collect(Collectors.toList());
        valueService.saveProductAttr(collect);

        // 5?????????spu???????????? sms_spu_bounds
        Bounds bounds = vo.getBounds();
        SpuBoundsTo spuBoundsTo = new SpuBoundsTo();
        BeanUtils.copyProperties(bounds, spuBoundsTo);
        spuBoundsTo.setSpuId(spuInfoEntity.getId());
        R r = couponFeignService.saveSpuBounds(spuBoundsTo);
        if (r.getCode() != 0) {
            log.error("??????????????????????????????");
        }

        // 6???????????????spu???????????????sku??????
        // 6.1???sku???????????? pms_sku_info
        List<Skus> skus = vo.getSkus();
        if (skus != null && skus.size() > 0) {
            skus.forEach((sku) -> {
                String defaultImg = "";
                for (Images image : sku.getImages()) {
                    if (image.getDefaultImg() == 1) {
                        defaultImg = image.getImgUrl();
                    }
                }
                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(sku, skuInfoEntity);
                skuInfoEntity.setBrandId(spuInfoEntity.getBrandId());
                skuInfoEntity.setCatalogId(spuInfoEntity.getCatalogId());
                skuInfoEntity.setSaleCount(0L);
                skuInfoEntity.setSpuId(spuInfoEntity.getId());
                skuInfoEntity.setSkuDefaultImg(defaultImg);
                skuInfoService.saveSkuInfo(skuInfoEntity);
                // ???????????????????????????skuId
                Long skuId = skuInfoEntity.getSkuId();

                List<SkuImagesEntity> imagesEntities = sku.getImages().stream().map(image -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setImgUrl(image.getImgUrl());
                    skuImagesEntity.setDefaultImg(image.getDefaultImg());
                    return skuImagesEntity;
                }).filter((item) -> {
                    // ??????true???????????????false??????????????????
                    return StringUtils.isNotEmpty(item.getImgUrl());
                }).collect(Collectors.toList());

                // 6.2???sku???????????? pms_sku_images
                // ?????????????????????????????????
                skuImagesService.saveBatch(imagesEntities);

                // 6.3???sku?????????????????? pms_sku_sale_attr_value
                List<Attr> attr = sku.getAttr();
                List<SkuSaleAttrValueEntity> saleAttrValueEntities = attr.stream().map(a -> {
                    SkuSaleAttrValueEntity saleAttrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(a, saleAttrValueEntity);
                    saleAttrValueEntity.setSkuId(skuId);
                    return saleAttrValueEntity;
                }).collect(Collectors.toList());
                skuSaleAttrValueService.saveBatch(saleAttrValueEntities);

                // 6.4???sku???????????????????????? gulimall_sms.sms_sku_ladder\sms_sku_full_reduction\sms_member_price
                SkuReductionTo skuReductionTo = new SkuReductionTo();
                BeanUtils.copyProperties(sku, skuReductionTo);
                skuReductionTo.setSkuId(skuId);
                if (skuReductionTo.getFullCount() > 0
                        || skuReductionTo.getFullPrice().compareTo(new BigDecimal(0)) == 1) {
                    R r1 = couponFeignService.saveSkuReduction(skuReductionTo);
                    if (r1.getCode() != 0) {
                        log.error("??????????????????????????????");
                    }
                }
            });
        }
    }

    @Override
    public void saveBaseSpuInfo(SpuInfoEntity spuInfoEntity) {
        this.baseMapper.insert(spuInfoEntity);
    }

    /**
     * ??????????????????????????????
     *
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SpuInfoEntity> queryWrapper = new QueryWrapper<>();

        String key = (String) params.get("key");
        String status = (String) params.get("status");
        String brandId = (String) params.get("brandId");
        String catalogId = (String) params.get("catalogId");

        // and (id = xxx or spu_name like xxx)
        if (StringUtils.isNotEmpty(key)) {
            queryWrapper.and((w) -> {
                w.eq("id", key).or().like("spu_name", key);
            });
        }

        if (StringUtils.isNotEmpty(status)) {
            queryWrapper.eq("publish_status", status);
        }

        if (StringUtils.isNotEmpty(brandId)) {
            queryWrapper.eq("brand_id", brandId);
        }

        if (StringUtils.isNotEmpty(catalogId)) {
            queryWrapper.eq("catalog_id", catalogId);
        }

        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void up(Long spuId) {
        // ??????spuId??????sku
        List<SkuInfoEntity> skus = skuInfoService.getSkusBySpuId(spuId);
        List<Long> skuIds = skus.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());

        List<ProductAttrValueEntity> baseAttrs = valueService.baseAttrListforspu(spuId);
        // ??????spuId?????????????????????id
        List<Long> attrIds = baseAttrs.stream().map(attrId -> {
            return attrId.getAttrId();
        }).collect(Collectors.toList());
        // ?????????????????????????????????id
        List<Long> searchAttrIds = attrService.selectSearchAttrIds(attrIds);
        Set<Long> set = new HashSet<>(searchAttrIds);
        // ???????????????????????????id???????????????????????????
        List<SkuEsModel.Attrs> attrs = baseAttrs.stream().filter(attr -> {
            return set.contains(attr.getAttrId());
        }).map(item -> {
            SkuEsModel.Attrs attrs1 = new SkuEsModel.Attrs();
            BeanUtils.copyProperties(item, attrs1);
            return attrs1;
        }).collect(Collectors.toList());

        // TODO 1?????????????????????????????????????????????????????????
        Map<Long, Boolean> stockMap = null;
        try {
            R r = wareFeignService.getSkusHasStock(skuIds);
            TypeReference<List<SkuHasStockTo>> typeReference = new TypeReference<List<SkuHasStockTo>>() {};
            List<SkuHasStockTo> data = r.getData(typeReference);
            stockMap = data.stream().collect(Collectors.toMap(SkuHasStockTo::getSkuId, item -> item.getHasStock()));
        } catch (Exception e) {
            log.error("????????????????????????{}", e);
        }

        // ????????????sku??????
        Map<Long, Boolean> map = stockMap;
        List<SkuEsModel> upProducts = skus.stream().map(sku -> {
            SkuEsModel esModel = new SkuEsModel();
            BeanUtils.copyProperties(sku, esModel);
            esModel.setSkuPrice(sku.getPrice());
            esModel.setSkuImg(sku.getSkuDefaultImg());

            // ???????????????????????????
            if (map == null) {
                esModel.setHasStock(true);
            } else {
                esModel.setHasStock(map.get(sku.getSkuId()));
            }

            // TODO 2??????????????????0
            esModel.setHotScore(0L);

            // TODO 3????????????????????????????????????
            BrandEntity brand = brandService.getById(esModel.getBrandId());
            esModel.setBrandName(brand.getName());
            esModel.setBrandImg(brand.getLogo());
            CategoryEntity category = categoryService.getById(esModel.getCatalogId());
            esModel.setCatalogName(category.getName());

            // TODO 4???????????????sku????????????????????????????????????
            esModel.setAttrsList(attrs);
            return esModel;
        }).collect(Collectors.toList());

        // ??????????????????es??????
        R r = searchFeignService.productStatusUp(upProducts);
        if (r.getCode() == 0) {
            // TODO ???????????????????????????????????????
            this.baseMapper.updateSpuStatus(spuId, ProductConst.StatusEnum.UP.getCode());
        } else {
            // TODO ?????????????????????????????????????????????
            /**
             * Feign????????????
             * 1???????????????????????????????????????json
             *  RequestTemplate template = buildTemplateFromArgs.create(argv);
             * 2???????????????????????????????????????????????????????????????
             *  executeAnaDecode(template);
             * 3?????????????????????????????????
             */
        }
    }

    @Override
    public SpuInfoEntity getSpuBySkuId(Long skuId) {
        SkuInfoEntity byId = skuInfoService.getById(skuId);
        Long spuId = byId.getSpuId();
        SpuInfoEntity spuInfoEntity = this.baseMapper.selectById(spuId);
        return spuInfoEntity;
    }
}