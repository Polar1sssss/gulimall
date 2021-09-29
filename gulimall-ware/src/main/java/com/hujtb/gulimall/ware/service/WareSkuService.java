package com.hujtb.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hujtb.common.to.OrderTo;
import com.hujtb.common.to.StockLockedTo;
import com.hujtb.common.utils.PageUtils;
import com.hujtb.gulimall.ware.entity.WareSkuEntity;
import com.hujtb.common.to.SkuHasStockTo;
import com.hujtb.gulimall.ware.vo.LockStockResult;
import com.hujtb.gulimall.ware.vo.WareSkuLockVo;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author hujtb
 * @email hujtb@qq.com
 * @date 2021-07-08 18:00:17
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStockTo> getSkusHasStock(List<Long> ids);

    Boolean orderLockStock(WareSkuLockVo vo);

    void unLockStock(StockLockedTo to);

    void unLockStock(OrderTo orderTo);
}

