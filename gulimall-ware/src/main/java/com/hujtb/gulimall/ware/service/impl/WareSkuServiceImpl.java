package com.hujtb.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.hujtb.common.exception.NoStockException;
import com.hujtb.common.to.OrderTo;
import com.hujtb.common.to.StockDetailTo;
import com.hujtb.common.to.StockLockedTo;
import com.hujtb.common.utils.R;
import com.hujtb.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.hujtb.gulimall.ware.entity.WareOrderTaskEntity;
import com.hujtb.gulimall.ware.feign.OrderFeignService;
import com.hujtb.gulimall.ware.feign.ProductFeignService;
import com.hujtb.common.to.SkuHasStockTo;
import com.hujtb.gulimall.ware.service.WareOrderTaskDetailService;
import com.hujtb.gulimall.ware.service.WareOrderTaskService;
import com.hujtb.gulimall.ware.vo.OrderItemVo;
import com.hujtb.gulimall.ware.vo.WareSkuLockVo;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
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
import org.springframework.transaction.annotation.Transactional;

@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    WareSkuDao wareSkuDao;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    WareOrderTaskService taskService;

    @Autowired
    WareOrderTaskDetailService detailService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    OrderFeignService orderFeignService;

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

    /**
     * 根据订单锁库存
     * (rollbackFor = NoStockException.class)不写的话默认也是遇到运行时异常回滚
     * 解锁场景：
     * 1.下订单成功，订单过期没有支付被系统自动取消或被用户手动取消
     * 2.下订单成功，库存锁定成功，接下来的业务执行失败，导致订单回滚，之前的锁库存操作需要回滚。seata太慢了，使用最终一致性
     *
     * @param vo
     * @return
     */
    @Transactional
    @Override
    public Boolean orderLockStock(WareSkuLockVo vo) {
        // 保存库存工作单详情
        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(vo.getOrderSn());
        taskService.save(taskEntity);

        // 找到每个商品对应的仓库
        List<OrderItemVo> locks = vo.getLocks();
        List<SkuWareHasStock> stocks = locks.stream().map(lock -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = lock.getSkuId();
            stock.setSkuId(skuId);
            stock.setNum(lock.getCount());
            List<Long> wareIds = wareSkuDao.listWareHasStock(skuId);
            stock.setWareId(wareIds);
            return stock;
        }).collect(Collectors.toList());

        // 锁定库存
        for (SkuWareHasStock hasStock : stocks) {
            // 默认商品是未锁定状态
            Boolean locked = false;
            Long skuId = hasStock.getSkuId();
            Integer num = hasStock.getNum();
            // 拥有库存的仓库
            List<Long> wareIds = hasStock.getWareId();
            if (wareIds == null || wareIds.size() == 0) {
                throw new NoStockException(skuId);
            }

            for (Long wareId : wareIds) {
                // rows==1表示影响到一行数据，锁库存成功
                Long rows = wareSkuDao.lockSkuStock(skuId, wareId, num);
                if (rows == 1) {
                    // 库存锁定成功，向mq发送消息（被锁定商品的库存订单详情）
                    WareOrderTaskDetailEntity detailEntity = new WareOrderTaskDetailEntity(
                            null,
                            skuId,
                            "",
                            hasStock.getNum(),
                            taskEntity.getId(),
                            wareId,
                            1
                    );
                    detailService.save(detailEntity);
                    StockLockedTo stockLockedTo = new StockLockedTo();
                    stockLockedTo.setId(taskEntity.getId());
                    StockDetailTo stockDetailTo = new StockDetailTo();
                    BeanUtils.copyProperties(detailEntity, stockDetailTo);
                    stockLockedTo.setDetailTo(stockDetailTo);
                    rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", stockLockedTo);
                    locked = true;
                    break;
                } else {
                    continue;
                }
            }

            // 只要有一件商品没有锁定成功，后面的商品也不用锁了
            if (!locked) {
                throw new NoStockException(skuId);
            }
        }
        return true;
    }

    /**
     * 监听器处理解锁库存方法
     *
     * @param to
     */
    @Override
    public void unLockStock(StockLockedTo to) {
        Long id = to.getId();
        StockDetailTo detailTo = to.getDetailTo();
        Long detailId = detailTo.getId();
        Long skuId = detailTo.getSkuId();
        Long wareId = detailTo.getWareId();
        Integer num = detailTo.getSkuNum();

        WareOrderTaskDetailEntity detailEntity = detailService.getById(detailId);
        if (detailEntity != null) {
            /*
                解锁操作，但是要根据订单情况进行判断
                1、没有这个订单，必须解锁
                2、有这个订单，且状态是已取消，需要解锁库存；其他状态无需解锁库存
             */
            WareOrderTaskEntity taskEntity = taskService.getById(id);
            String orderSn = taskEntity.getOrderSn();

            // 根据订单号查询订单状态
            R r = orderFeignService.getOrderStatus(orderSn);
            if (r.getCode() == 0) {
                OrderTo data = r.getData(new TypeReference<OrderTo>() {
                });
                if (data == null || data.getStatus() == 4) {
                    // 订单被取消了，解锁库存，只有工作单中库存状态是已锁定才能解锁
                    if (detailEntity.getLockStatus() == 1) {
                        releaseLockedStock(skuId, wareId, num, detailId);
                    }
                }
            } else {
                throw new RuntimeException("远程调用失败");
            }
        } else {
            // 没有详情单，证明库存服务本身出现问题，数据已经回滚了，这种情况无需解锁
        }
    }

    /**
     * 防止订单服务卡顿，库存的延时队列优先到期，查询订单的状态为新建状态，就永远不会解锁库存
     * 订单工作单详情是一个list，有一个解锁失败就应该回滚
     *
     * @param orderTo
     */
    @Transactional
    @Override
    public void unLockStock(OrderTo orderTo) {
        String orderSn = orderTo.getOrderSn();
        // 查一下最新的库存解锁状态，防止重复解锁
        WareOrderTaskEntity task = taskService.getOrderTaskBysn(orderSn);
        Long id = task.getId();
        // 按照工作单id找到所有没有解锁【lock_status=1】的库存进行解锁
        List<WareOrderTaskDetailEntity> entities = detailService.list(new QueryWrapper<WareOrderTaskDetailEntity>().eq("task_id", id).eq("lock_status", 1));
        for (WareOrderTaskDetailEntity entity : entities) {
            Long skuId = entity.getSkuId();
            Long wareId = entity.getWareId();
            Integer skuNum = entity.getSkuNum();
            Long detailId = entity.getId();
            releaseLockedStock(skuId, wareId, skuNum, detailId);
        }
    }

    /**
     * 解锁锁定的库存
     *
     * @param skuId
     * @param wareId
     * @param num
     * @param taskDetailId
     */
    private void releaseLockedStock(Long skuId, Long wareId, Integer num, Long taskDetailId) {
        wareSkuDao.releaseLockedStock(skuId, wareId, num);
        // 解锁成功之后，更新库存工作单
        WareOrderTaskDetailEntity detailEntity = new WareOrderTaskDetailEntity();
        detailEntity.setId(taskDetailId);
        detailEntity.setLockStatus(2);
        detailService.updateById(detailEntity);
    }

    @Data
    class SkuWareHasStock {
        // 商品id
        private Long skuId;
        // 锁定多少件
        private Integer num;
        // 商品在哪个库存中有
        private List<Long> wareId;
    }

}