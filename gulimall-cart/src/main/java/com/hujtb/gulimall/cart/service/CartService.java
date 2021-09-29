package com.hujtb.gulimall.cart.service;

import com.hujtb.gulimall.cart.vo.CartItemVo;
import com.hujtb.gulimall.cart.vo.CartVo;

import java.util.List;

public interface CartService {

    CartItemVo addToCart(Long skuId, Integer num);

    CartItemVo getCartItem(Long skuId);

    CartVo getCartInfo();

    void clearCart(String cartKey);

    void checkItem(Long skuId, Integer checked);

    void countItem(Long skuId, Integer num);

    void deleteItem(Long skuId);

    List<CartItemVo> getcurrentUserCartItems();
}
