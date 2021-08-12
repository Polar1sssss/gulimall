package com.hujtb.gulimall.ware.service.impl;

import com.hujtb.common.utils.R;
import com.hujtb.gulimall.ware.feign.ProductFeignService;
import com.hujtb.common.to.SkuHasStockTo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hujtb.common.utils.PageUtils;
import com.hujtb.common.utils.Query;

import com.hujtb.gulimall.ware.dao.WareSkuDao;
import com.hujtb.gulimall.ware.entity.WareSkuEntity;
import com.hujtb.gulimall.ware.service.WareSkuService;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    WareSkuDao wareSkuDao;

    @Autowired
    ProductFeignService productFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> queryWrapper = new QueryWrapper<>();
        String skuId = (String) params.get("skuId");
        String wareId = (String) params.get("wareId");

        if (StringUtils.isNotEmpty(skuId)) {
            queryWrapper.eq("sku_id", skuId);
        }

        if (StringUtils.isNotEmpty(wareId)) {
            queryWrapper.eq("ware_id", wareId);
        }

        IPage<WareSkuEntity> page = this.page(new Query<WareSkuEntity>().getPage(params), queryWrapper);

        return new PageUtils(page);
    }

    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        List<WareSkuEntity> entity = wareSkuDao.selectList(new QueryWrapper<WareSkuEntity>()
                .eq("sku_id", skuId)
                .eq("ware_id", wareId));
        if (entity == null || entity.size() == 0) {
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setStockLocked(0);
            // 远程获取名字，如果失败无需回滚，所以放到try catch里面
            try {
                R info = productFeignService.info(skuId);
                if (info.getCode() == 0) {
                    Map<String, Object> map = (Map<String, Object>) info.get("skuInfo");
                    String name = (String) map.get("skuName");
                    wareSkuEntity.setSkuName(name);
                }
            } catch (Exception e) {
            }
            wareSkuDao.insert(wareSkuEntity);
        }
        wareSkuDao.addStock(skuId, wareId, skuNum);
    }

    @Override
    public List<SkuHasStockTo> getSkusHasStock(List<Long> ids) {
        List<SkuHasStockTo> stockVos = ids.stream().map(id -> {
            SkuHasStockTo SkuHasStockTo = new SkuHasStockTo();
            Long count = this.baseMapper.getSkuStock(id);
            // 动态获取的对象一定要做非空判断
            SkuHasStockTo.setHasStock(count == null ? false : count > 0);
            return SkuHasStockTo;
        }).collect(Collectors.toList());
        return stockVos;
    }

}