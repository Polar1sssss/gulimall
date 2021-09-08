package com.hujtb.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hujtb.common.utils.PageUtils;
import com.hujtb.gulimall.product.entity.SkuInfoEntity;
import com.hujtb.gulimall.product.vo.SkuItemVo;

import java.util.List;
import java.util.Map;

/**
 * sku信息
 *
 * @author hujtb
 * @email hujtb@qq.com
 * @date 2021-07-08 17:55:32
 */
public interface SkuInfoService extends IService<SkuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSkuInfo(SkuInfoEntity skuInfoEntity);

    PageUtils queryPageByCondition(Map<String, Object> params);

    List<SkuInfoEntity> getSkusBySpuId(Long spuId);

    SkuItemVo item(Long skuId);
}

