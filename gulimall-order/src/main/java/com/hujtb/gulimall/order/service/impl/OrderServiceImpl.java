package com.hujtb.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.hujtb.common.exception.NoStockException;
import com.hujtb.common.to.OrderTo;
import com.hujtb.common.to.SeckillOrderTo;
import com.hujtb.common.to.SkuHasStockTo;
import com.hujtb.common.utils.R;
import com.hujtb.common.vo.MemberRespVo;
import com.hujtb.gulimall.order.constant.OrderConst;
import com.hujtb.gulimall.order.entity.OrderItemEntity;
import com.hujtb.gulimall.order.entity.PaymentInfoEntity;
import com.hujtb.gulimall.order.enume.OrderStatusEnum;
import com.hujtb.gulimall.order.feign.CartFeignService;
import com.hujtb.gulimall.order.feign.MemberFeignService;
import com.hujtb.gulimall.order.feign.ProductFeignService;
import com.hujtb.gulimall.order.feign.WareFeignService;
import com.hujtb.gulimall.order.interceptor.LoginUserInterceptor;
import com.hujtb.gulimall.order.service.OrderItemService;
import com.hujtb.gulimall.order.service.PaymentInfoService;
import com.hujtb.gulimall.order.vo.*;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hujtb.common.utils.PageUtils;
import com.hujtb.common.utils.Query;

import com.hujtb.gulimall.order.dao.OrderDao;
import com.hujtb.gulimall.order.entity.OrderEntity;
import com.hujtb.gulimall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    private ThreadLocal<OrderSubmitVo> submitVoThreadLocal = new ThreadLocal<>();

    @Autowired
    ThreadPoolExecutor executor;

    @Autowired
    MemberFeignService memberFeignService;

    @Autowired
    CartFeignService cartFeignService;

    @Autowired
    WareFeignService wareFeignService;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    OrderItemService orderItemService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    PaymentInfoService paymentInfoService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );
        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {
        MemberRespVo memberRespVo = LoginUserInterceptor.threadLocal.get();
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>().eq("member_id", memberRespVo.getId()).orderByDesc("id")
        );
        List<OrderEntity> collect = page.getRecords().stream().map(order -> {
            List<OrderItemEntity> order_sn = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", order.getOrderSn()));
            // ?????????????????????????????????
            order.setOrderItemEntityList(order_sn);
            return order;
        }).collect(Collectors.toList());
        page.setRecords(collect);

        return new PageUtils(page);
    }

    @Override
    public String handlePayResult(PayAsyncVo vo) {
        // 1?????????????????????
        PaymentInfoEntity infoEntity = new PaymentInfoEntity();
        infoEntity.setAlipayTradeNo(vo.getTrade_no());
        String out_trade_no = vo.getOut_trade_no();
        infoEntity.setOrderSn(out_trade_no);
        infoEntity.setPaymentStatus(vo.getTrade_status());
        infoEntity.setCallbackTime(vo.getNotify_time());
        paymentInfoService.save(infoEntity);
        
        // 2???????????????????????????
        if (vo.getTrade_status().equals("TRADE_SUCCESS") || vo.getTrade_status().equals("TRADE_FINISHED")) {
            this.baseMapper.updateOrderStatus(out_trade_no, OrderStatusEnum.PAYED.getCode());
        }
        return "success";
    }

    @Override
    public void createSeckillOrder(SeckillOrderTo orderTo) {
        // ??????????????????
        OrderEntity entity = new OrderEntity();
        entity.setOrderSn(orderTo.getOrderSn());
        entity.setMemberId(orderTo.getMemberId());
        entity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        BigDecimal multiply = orderTo.getSeckillPrice().multiply(new BigDecimal("" + orderTo.getNum()));
        entity.setPayAmount(multiply);
        this.save(entity);

        // ?????????????????????
        OrderItemEntity itemEntity = new OrderItemEntity();
        itemEntity.setOrderSn(orderTo.getOrderSn());
        itemEntity.setRealAmount(multiply);
        itemEntity.setSkuQuantity(orderTo.getNum());
        orderItemService.save(itemEntity);
    }

    @Override
    public OrderConfirmVo getConfirm() {
        MemberRespVo respVo = LoginUserInterceptor.threadLocal.get();
        Long id = respVo.getId();
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        CompletableFuture<Void> getAddressTask = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            // ??????????????????????????????
            List<MemberAddressVo> address = memberFeignService.getAddress(id);
            confirmVo.setAddress(address);
        }, executor).thenRunAsync(() -> {
            List<OrderItemVo> items = confirmVo.getItems();
            List<Long> collect = items.stream().map(item -> {
                Long skuId = item.getSkuId();
                return skuId;
            }).collect(Collectors.toList());
            R skusHasStock = wareFeignService.getSkusHasStock(collect);
            if (skusHasStock != null) {
                List<SkuHasStockTo> skus = skusHasStock.getData(new TypeReference<List<SkuHasStockTo>>() {
                });
                if (skus != null) {
                    // List??????Map
                    Map<Long, Boolean> map = skus.stream().collect(Collectors.toMap(SkuHasStockTo::getSkuId, SkuHasStockTo::getHasStock));
                    confirmVo.setStocks(map);
                }
            }
        }, executor);

        CompletableFuture<Void> getItemsTask = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            // ???????????????????????????
            List<OrderItemVo> items = cartFeignService.getcurrentUserCartItems();
            confirmVo.setItems(items);
        }, executor);
        // feign??????????????????????????????????????????????????????

        confirmVo.setIntegration(respVo.getIntegration());
        try {
            CompletableFuture.allOf(getAddressTask, getItemsTask).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        // ????????????
        String token = UUID.randomUUID().toString().replace("-", "");
        // ???????????????????????????
        confirmVo.setOrderToken(token);
        // ???redis?????????????????????
        redisTemplate.opsForValue().set(OrderConst.USER_ORDER_TOKEN_PREFIX + id, token, 30, TimeUnit.MINUTES);
        return confirmVo;
    }

    /**
     * @param submitVo
     * @return
     * @Transactional???????????????????????????????????????????????????????????????
     */
    @GlobalTransactional
    @Override
    public OrderSubmitResponseVo submitOrder(OrderSubmitVo submitVo) {
        submitVoThreadLocal.set(submitVo);
        MemberRespVo respVo = LoginUserInterceptor.threadLocal.get();
        Long id = respVo.getId();
        OrderSubmitResponseVo response = new OrderSubmitResponseVo();
        response.setCode(0);
        // ????????????????????????????????????????????????????????????
        // ???????????????0 - ???????????????1 - ????????????
        String script = "if redis.call('get', KEY[1]) == ARGV[1] then return redis.call('del', KEY[1]) else return 0 end";
        String orderToken = submitVo.getOrderToken();
        Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),
                Arrays.asList(OrderConst.USER_ORDER_TOKEN_PREFIX + id), orderToken);
        if (result == 0L) {
            response.setCode(1);
            return response;
        } else {
            // ??????????????????
            // ???????????????????????????????????????????????????????????????
            OrderCreateVo orderCreateVo = createOrder();
            // ?????????????????????
            BigDecimal payAmount = orderCreateVo.getOrder().getPayAmount();
            // ??????????????????
            BigDecimal payPrice = orderCreateVo.getPayPrice();
            if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {
                // ????????????
                // TODO ????????????
                saveOrder(orderCreateVo);
                // ???????????????????????????????????????????????????
                WareSkuLockVo wareSkuLockVo = new WareSkuLockVo();
                wareSkuLockVo.setOrderSn(orderCreateVo.getOrder().getOrderSn());
                List<OrderItemVo> locks = orderCreateVo.getOrderItems().stream().map(item -> {
                    OrderItemVo orderItemVo = new OrderItemVo();
                    // ???????????????
                    orderItemVo.setSkuId(item.getSkuId());
                    // ?????????
                    orderItemVo.setCount(item.getSkuQuantity());
                    orderItemVo.setTitle(item.getSkuName());
                    return orderItemVo;
                }).collect(Collectors.toList());
                wareSkuLockVo.setLocks(locks);
                // TODO ???????????????
                R orderLockStock = wareFeignService.orderLockStock(wareSkuLockVo);
                if (orderLockStock.getCode() != 0) {
                    response.setCode(3);
                    String msg = (String) orderLockStock.get("msg");
                    throw new NoStockException(msg);
                } else {
                    response.setOrder(orderCreateVo.getOrder());
                    // ????????????????????????????????????mq
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", orderCreateVo.getOrder());
                    return response;
                }
            } else {
                response.setCode(2);
                return response;
            }
        }
    }

    @Override
    public OrderEntity getOrderStatusBySn(String orderSn) {
        OrderEntity entity = this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
        return entity;
    }

    /**
     * ????????????
     *
     * @param entity
     */
    @Override
    public void closeOrder(OrderEntity entity) {
        // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        OrderEntity order = this.getById(entity.getId());
        if (order.getStatus() == OrderStatusEnum.CREATE_NEW.getCode()) {
            OrderEntity update = new OrderEntity();
            update.setId(entity.getId());
            update.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(update);
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(order, orderTo);
            // ????????????????????????mq??????????????????
            try {
                // TODO ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other", orderTo);
            } catch (Exception e) {
            }

        }
    }

    @Override
    public PayVo getOrderPay(String orderSn) {
        PayVo payVo = new PayVo();
        OrderEntity order = this.getOrderStatusBySn(orderSn);
        payVo.setOut_trade_no(orderSn);
        payVo.setTotal_amount(order.getTotalAmount().setScale(2, BigDecimal.ROUND_UP).toString());
        List<OrderItemEntity> list = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
        OrderItemEntity entity = list.get(0);
        payVo.setSubject(entity.getSkuName());
        payVo.setBody(entity.getSkuAttrsVals());
        return payVo;
    }

    /**
     * ??????????????????
     *
     * @param orderCreateVo
     */
    private void saveOrder(OrderCreateVo orderCreateVo) {
        OrderEntity order = orderCreateVo.getOrder();
        order.setModifyTime(new Date());
        this.save(order);

        List<OrderItemEntity> orderItems = orderCreateVo.getOrderItems();
        orderItemService.saveBatch(orderItems);
    }

    /**
     * ????????????
     *
     * @return
     */
    private OrderCreateVo createOrder() {
        OrderCreateVo createVo = new OrderCreateVo();
        // ???????????????
        String orderSn = IdWorker.getTimeId();
        OrderEntity entity = buildOrder(orderSn);
        // ?????????????????????
        List<OrderItemEntity> orderItemEntities = buildOrderItems(orderSn);
        createVo.setOrder(entity);
        createVo.setOrderItems(orderItemEntities);
        computePrice(entity, orderItemEntities);
        return createVo;
    }

    /**
     * ??????????????????
     * ????????????????????????????????????????????????
     *
     * @param entity
     * @param orderItemEntities
     */
    private void computePrice(OrderEntity entity, List<OrderItemEntity> orderItemEntities) {
        BigDecimal totalAmount = new BigDecimal(0.0);
        BigDecimal coupon = new BigDecimal(0.0);
        BigDecimal integration = new BigDecimal(0.0);
        BigDecimal promotion = new BigDecimal(0.0);
        Integer gift = 0;
        Integer growth = 0;
        for (OrderItemEntity item : orderItemEntities) {
            coupon = coupon.add(item.getCouponAmount());
            integration = integration.add(item.getIntegrationAmount());
            promotion = promotion.add(item.getPromotionAmount());
            totalAmount = totalAmount.add(item.getRealAmount());
            gift = gift + item.getGiftIntegration();
            growth = growth + item.getGiftIntegration();
        }
        entity.setTotalAmount(totalAmount);
        entity.setPayAmount(totalAmount.add(entity.getFreightAmount()));
        entity.setPromotionAmount(promotion);
        entity.setCouponAmount(coupon);
        entity.setIntegrationAmount(integration);
        entity.setIntegration(gift);
        entity.setGrowth(growth);
    }

    /**
     * ??????????????????
     *
     * @return
     */
    private OrderEntity buildOrder(String orderSn) {
        MemberRespVo memberRespVo = LoginUserInterceptor.threadLocal.get();
        Long id = memberRespVo.getId();
        OrderSubmitVo submitVo = submitVoThreadLocal.get();
        OrderEntity entity = new OrderEntity();
        entity.setOrderSn(orderSn);
        entity.setMemberId(id);
        // ????????????????????????
        R fare = wareFeignService.getFare(submitVo.getAddrId());
        FareVo fareData = fare.getData(new TypeReference<FareVo>() {
        });
        // ??????
        entity.setFreightAmount(fareData.getFare());
        // ???????????????
        entity.setReceiverCity(fareData.getAddressVo().getCity());
        entity.setReceiverDetailAddress(fareData.getAddressVo().getDetailAddress());
        entity.setReceiverName(fareData.getAddressVo().getName());
        entity.setReceiverPhone(fareData.getAddressVo().getPhone());
        entity.setReceiverPostCode(fareData.getAddressVo().getPostCode());
        entity.setReceiverProvince(fareData.getAddressVo().getProvince());
        entity.setReceiverRegion(fareData.getAddressVo().getRegion());

        entity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        entity.setAutoConfirmDay(7);
        return entity;
    }

    /**
     * ???????????????????????????
     *
     * @return
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        List<OrderItemVo> orderItemVos = cartFeignService.getcurrentUserCartItems();
        List<OrderItemEntity> OrderItemEntities = null;
        if (orderItemVos != null && orderItemVos.size() > 0) {
            OrderItemEntities = orderItemVos.stream().map(cartItem -> {
                OrderItemEntity orderItemEntity = buildOrderItem(cartItem);
                orderItemEntity.setOrderSn(orderSn);
                return orderItemEntity;
            }).collect(Collectors.toList());
        }
        return OrderItemEntities;
    }

    /**
     * ??????????????????????????????
     *
     * @param cartItem
     * @return
     */
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity orderItem = new OrderItemEntity();
        // ????????????
        // ??????spu??????
        Long skuId = cartItem.getSkuId();
        SpuInfoVo spuInfo = productFeignService.getSpuBySkuId(skuId);
        orderItem.setSpuId(spuInfo.getId());
        orderItem.setSpuBrand(spuInfo.getBrandId().toString());
        orderItem.setSpuName(spuInfo.getSpuName());
        orderItem.setCategoryId(spuInfo.getCatalogId());

        // ??????sku??????
        orderItem.setSkuId(cartItem.getSkuId());
        orderItem.setSkuName(cartItem.getTitle());
        orderItem.setSkuPic(cartItem.getImage());
        orderItem.setSkuPrice(cartItem.getPrice());
        // ??????????????????????????????????????????????????????
        String skuAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";");
        orderItem.setSkuAttrsVals(skuAttr);
        orderItem.setSkuQuantity(cartItem.getCount());
        // ????????????????????????
        // ????????????
        orderItem.setGiftGrowth(cartItem.getPrice().intValue() * cartItem.getCount());
        orderItem.setGiftIntegration(cartItem.getPrice().intValue() * cartItem.getCount());

        orderItem.setPromotionAmount(new BigDecimal(0));
        orderItem.setCouponAmount(new BigDecimal(0));
        orderItem.setIntegrationAmount(new BigDecimal(0));
        BigDecimal skuQuantity = new BigDecimal(orderItem.getSkuQuantity().toString());
        BigDecimal itemPrice = orderItem.getSkuPrice().multiply(skuQuantity);
        BigDecimal realPrice = itemPrice.subtract(orderItem.getPromotionAmount())
                .subtract(orderItem.getCouponAmount())
                .subtract(orderItem.getIntegrationAmount());
        // ???????????????????????????
        orderItem.setRealAmount(realPrice);
        return orderItem;
    }

}