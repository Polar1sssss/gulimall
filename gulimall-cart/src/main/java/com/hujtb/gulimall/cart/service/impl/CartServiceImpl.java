package com.hujtb.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.hujtb.common.utils.R;
import com.hujtb.gulimall.cart.feign.ProductFeignService;
import com.hujtb.gulimall.cart.interceptor.CartInterceptor;
import com.hujtb.gulimall.cart.service.CartService;
import com.hujtb.gulimall.cart.vo.CartVo;
import com.hujtb.gulimall.cart.vo.SkuInfoVo;
import com.hujtb.gulimall.cart.vo.CartItemVo;
import com.hujtb.gulimall.cart.vo.UserInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * 购物车服务类
 * 需要判断是否登录，需要引入spring-session
 */
@Slf4j
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    ThreadPoolExecutor executor;

    private final String CART_PREFIX = "gulimall:cart:";

    @Override
    public CartItemVo addToCart(Long skuId, Integer num) {
        // 操作gulimall:cart:用户 这个key下面的商品
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItemVo itemVo = new CartItemVo();
        String res = (String) cartOps.get(skuId.toString());
        // 如果是新商品，要设置所有的属性，如果是已经添加过的商品，只需要修改数量
        if (StringUtils.isNotEmpty(res)) {
            // 远程查询要添加到购物车的商品信息
            CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
                R skuInfo = productFeignService.getSkuInfo(skuId);
                SkuInfoVo data = skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                });
                itemVo.setChecked(true);
                itemVo.setCount(num);
                itemVo.setImage(data.getSkuDefaultImg());
                itemVo.setPrice(data.getPrice());
                itemVo.setTitle(data.getSkuTitle());
                itemVo.setSkuId(skuId);
            }, executor);

            // 远程查询商品属性组合
            CompletableFuture<Void> getskuSaleAttrValuesTask = CompletableFuture.runAsync(() -> {
                List<String> skuSaleAttrValues = productFeignService.getSkuSaleAttrValues(skuId);
                itemVo.setSkuAttr(skuSaleAttrValues);
            }, executor);

            try {
                CompletableFuture.allOf(getSkuInfoTask, getskuSaleAttrValuesTask).get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            String s = JSON.toJSONString(itemVo);
            cartOps.put(skuId.toString(), s);
        } else {
            CartItemVo cartItemVo = JSON.parseObject(res, CartItemVo.class);
            Integer count = cartItemVo.getCount();
            itemVo.setCount(count + num);
            String s = JSON.toJSONString(itemVo);
            cartOps.put(skuId, s);
        }
        return itemVo;
    }

    /**
     * 获取加入购物车成功后的成功页列表
     *
     * @param skuId
     * @return
     */
    @Override
    public CartItemVo getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String res = (String) cartOps.get(skuId.toString());
        CartItemVo cartItemVo = JSON.parseObject(res, CartItemVo.class);
        return cartItemVo;
    }

    /**
     * 获取购物车里面所有的购物项
     *
     * @return
     */
    @Override
    public CartVo getCartInfo() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        CartVo cartVo = new CartVo();
        if (userInfoTo.getUserId() != null) {
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            String tempKey = CART_PREFIX + userInfoTo.getUserKey();
            // 如果临时购物车还没有合并，登录后将临时购物车合并，并清空临时购物车
            List<CartItemVo> tempCartItems = getCartItemsFromRedis(tempKey);
            if (tempCartItems != null && tempCartItems.size() > 0) {
                for (CartItemVo item : tempCartItems) {
                    addToCart(item.getSkuId(), item.getCount());
                }
                // 清空临时购物车
                clearCart(tempKey);
            }
            // 该用户购物车中所有的购物项
            List<CartItemVo> cartItems = getCartItemsFromRedis(cartKey);
            cartVo.setItems(cartItems);
        } else {
            // 没登录状态，直接返回购物车里的商品信息
            String cartKey = CART_PREFIX + userInfoTo.getUserKey();
            List<CartItemVo> cartItems = getCartItemsFromRedis(cartKey);
            cartVo.setItems(cartItems);
        }
        return cartVo;
    }

    @Override
    public void clearCart(String cartKey) {
        BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(cartKey);
        redisTemplate.delete(cartKey);
    }

    @Override
    public void checkItem(Long skuId, Integer checked) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String o = (String) cartOps.get(skuId.toString());
        CartItemVo cartItemVo = JSON.parseObject(o, CartItemVo.class);
        cartItemVo.setChecked(checked == 1);
        String s = JSON.toJSONString(cartItemVo);
        cartOps.put(skuId.toString(), s);
    }

    @Override
    public void countItem(Long skuId, Integer num) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String o = (String) cartOps.get(skuId.toString());
        CartItemVo cartItemVo = JSON.parseObject(o, CartItemVo.class);
        cartItemVo.setCount(num);
        String s = JSON.toJSONString(cartItemVo);
        cartOps.put(skuId.toString(), s);
    }

    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }

    @Override
    public List<CartItemVo> getcurrentUserCartItems() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if (userInfoTo.getUserId() == null) {
            return null;
        } else {
            Long userId = userInfoTo.getUserId();
            String cartKey = CART_PREFIX + userId;
            // 获取该用户购物车里所有商品
            List<CartItemVo> cartItems = getCartItemsFromRedis(cartKey);
            List<CartItemVo> collect = cartItems.stream()
                    .filter(item -> item.getChecked())
                    .map(item -> {
                        // 获取最新价格
                        R newPrice = productFeignService.getPriceById(item.getSkuId());
                        String data = (String) newPrice.get("data");
                        item.setPrice(new BigDecimal(data));
                        return item;
                    }).collect(Collectors.toList());
            return collect;
        }
    }

    /**
     * 获取购物车里面所有的购物项（redis中）
     *
     * @param cartKey
     * @return
     */
    private List<CartItemVo> getCartItemsFromRedis(String cartKey) {
        BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(cartKey);
        List<Object> values = ops.values();
        if (values != null && values.size() > 0) {
            List<CartItemVo> itemVos = values.stream().map((value) -> {
                CartItemVo cartItemVo = JSON.parseObject(value.toString(), CartItemVo.class);
                return cartItemVo;
            }).collect(Collectors.toList());
            return itemVos;
        }
        return null;
    }

    /**
     * 返回要操作的购物车，用户信息已经在里面了
     *
     * @return
     */
    private BoundHashOperations<String, Object, Object> getCartOps() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        String cartKey = "";
        if (userInfoTo.getUserId() != null) {
            // 登录用户
            cartKey = CART_PREFIX + userInfoTo.getUserId();
        } else {
            // 临时用户
            cartKey = CART_PREFIX + userInfoTo.getUserKey();
        }
        BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(cartKey);
        return ops;
    }
}
