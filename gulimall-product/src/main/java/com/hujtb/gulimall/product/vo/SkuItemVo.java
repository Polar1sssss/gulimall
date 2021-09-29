package com.hujtb.gulimall.product.vo;

import com.hujtb.gulimall.product.entity.SkuImagesEntity;
import com.hujtb.gulimall.product.entity.SkuInfoEntity;
import com.hujtb.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

@Data
public class SkuItemVo {

    private SkuInfoEntity info;
    private boolean hasStock = true;
    private List<SkuImagesEntity> images;
    private List<SkuItemSaleAttrVo> saleAttrs;
    private SpuInfoDescEntity spuDesc;
    private List<SpuItemAttrGroupVo> groupAttrs;
    private SeckillSkuVo seckillSkuVo;
}
