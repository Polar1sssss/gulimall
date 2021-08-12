package com.hujtb.gulimall.ware.service.impl;

import com.hujtb.common.constant.WareConst;
import com.hujtb.gulimall.ware.entity.PurchaseDetailEntity;
import com.hujtb.gulimall.ware.service.PurchaseDetailService;
import com.hujtb.gulimall.ware.service.WareSkuService;
import com.hujtb.gulimall.ware.vo.ItemDoneVo;
import com.hujtb.gulimall.ware.vo.MergeVo;
import com.hujtb.gulimall.ware.vo.PurchaseDoneVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hujtb.common.utils.PageUtils;
import com.hujtb.common.utils.Query;

import com.hujtb.gulimall.ware.dao.PurchaseDao;
import com.hujtb.gulimall.ware.entity.PurchaseEntity;
import com.hujtb.gulimall.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    @Autowired
    PurchaseDetailService purchaseDetailService;

    @Autowired
    WareSkuService wareSkuService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<PurchaseEntity> queryWrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        String status = (String) params.get("status");
        if (StringUtils.isNotEmpty(key)) {
            queryWrapper.eq("id", key);
        }
        if (StringUtils.isNotEmpty(status)) {
            queryWrapper.eq("status", status);
        }
        IPage<PurchaseEntity> page = this.page(new Query<PurchaseEntity>().getPage(params), queryWrapper );
        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageUnreceived(Map<String, Object> params) {
        Integer[] statusArray = {0, 1};
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>().in("status", Arrays.asList(statusArray))
        );

        return new PageUtils(page);
    }

    /**
     * 合并整单
     *
     * @param mergeVo
     */
    @Override
    @Transactional
    public void merge(MergeVo mergeVo) {
        // purchaseId:1,
        // item:[4, 5]
        Long purchaseId = mergeVo.getPurchaseId();
        if (purchaseId == null) {
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setStatus(WareConst.PurchaseStatusEnum.CREATED.getCode());
            purchaseEntity.setCreateTime(new Date());
            purchaseEntity.setUpdateTime(new Date());
            this.save(purchaseEntity);
            purchaseId = purchaseEntity.getId();
        }

        List<Long> items = mergeVo.getItems();
        Long finalPurchaseId = purchaseId;
        List<PurchaseDetailEntity> purchaseDetailEntities = items.stream().map(item -> {
            PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
            purchaseDetailEntity.setId(item);
            purchaseDetailEntity.setPurchaseId(finalPurchaseId);
            purchaseDetailEntity.setStatus(WareConst.PurchaseDetailStatusEnum.CREATED.getCode());
            return purchaseDetailEntity;
        }).filter(item -> {
            // 如果有采购单，它的状态必须是0或1才能合并采购需求
            return (item.getStatus() == WareConst.PurchaseStatusEnum.CREATED.getCode()
                    || item.getStatus() == WareConst.PurchaseStatusEnum.ASSIGNEND.getCode());
        }).collect(Collectors.toList());
        purchaseDetailService.updateBatchById(purchaseDetailEntities);

        // 更新采购单的更新时间
        PurchaseEntity purchaseEntity1 = new PurchaseEntity();
        purchaseEntity1.setId(purchaseId);
        purchaseEntity1.setUpdateTime(new Date());
        this.updateById(purchaseEntity1);

    }

    @Override
    public void received(List<Long> ids) {
        // 确认当前采购单是新建或已领取状态
        List<PurchaseEntity> purchaseEntities = ids.stream().map(item -> {
            PurchaseEntity byId = this.getById(item);
            return byId;
        }).filter(item -> {
            return (item.getStatus() == WareConst.PurchaseStatusEnum.CREATED.getCode()
                    || item.getStatus() == WareConst.PurchaseStatusEnum.ASSIGNEND.getCode());
        }).map(item -> {
            item.setStatus(WareConst.PurchaseStatusEnum.RECEIVED.getCode());
            item.setUpdateTime(new Date());
            return item;
        }).collect(Collectors.toList());
        // 改变采购单状态
        this.updateBatchById(purchaseEntities);
        // 改变采购项状态
        purchaseEntities.forEach(item -> {
            List<PurchaseDetailEntity> entities = purchaseDetailService.listDetailByPurchaseId(item.getId());
            List<PurchaseDetailEntity> detailEntities = entities.stream().map(item1 -> {
                PurchaseDetailEntity entity1 = new PurchaseDetailEntity();
                entity1.setId(item1.getId());
                entity1.setStatus(item1.getStatus());
                return entity1;
            }).collect(Collectors.toList());
            purchaseDetailService.updateBatchById(detailEntities);
        });
    }

    @Transactional
    @Override
    public void done(PurchaseDoneVo doneVo) {
        // 改变采购项状态
        Boolean flag = true;
        List<ItemDoneVo> itemDoneVos = doneVo.getItemDoneVo();
        List<PurchaseDetailEntity> updates = new ArrayList<>();
        PurchaseDetailEntity detail = new PurchaseDetailEntity();
        for (ItemDoneVo item : itemDoneVos) {
            if (item.getStatus() == WareConst.PurchaseDetailStatusEnum.FAILED.getCode()) {
                flag = false;
                detail.setStatus(item.getStatus());
            } else {
                detail.setStatus(WareConst.PurchaseDetailStatusEnum.FINISHED.getCode());
                // 成功采购之后入库
                PurchaseDetailEntity byId = purchaseDetailService.getById(item.getItemId());
                wareSkuService.addStock(byId.getSkuId(), byId.getWareId(), byId.getSkuNum());
            }
            detail.setId(item.getItemId());
            updates.add(detail);
        }
        purchaseDetailService.updateBatchById(updates);

        // 改变采购单状态
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(doneVo.getPurchaseId());
        purchaseEntity.setStatus(flag ? WareConst.PurchaseStatusEnum.FINISHED.getCode()
                : WareConst.PurchaseStatusEnum.HASERROR.getCode());
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);

    }
}