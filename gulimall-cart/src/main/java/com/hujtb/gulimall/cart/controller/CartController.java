package com.hujtb.gulimall.cart.controller;

import com.hujtb.gulimall.cart.interceptor.CartInterceptor;
import com.hujtb.gulimall.cart.service.CartService;
import com.hujtb.gulimall.cart.vo.CartItemVo;
import com.hujtb.gulimall.cart.vo.CartVo;
import com.hujtb.gulimall.cart.vo.UserInfoTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class CartController {

    @Autowired
    CartService cartService;

    /**
     * 跳转到购物车列表页
     * 浏览器中有一个cookie:user-key：标识用户身份，一个月后过期
     * 如果第一次使用购物车功能，会自动生成一个user-key
     * 之后每次访问都会带上这个user-key
     *
     * @return
     */
    @GetMapping("/cart.html")
    public String cartListPage(Model model) {
        CartVo cartVo = cartService.getCartInfo();
        model.addAttribute("cart", cartVo);
        return "cartList";
    }

    /**
     * 添加到购物车
     * ra.addAttribute("skuId", skuId)：将数据放在url后面
     * ra.addFlashAttribute()：将数据放到session中，可以从页面取出，只能取一次
     *
     * @return
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num, RedirectAttributes ra) {
        cartService.addToCart(skuId, num);
        ra.addAttribute("skuId", skuId);
        // 商品添加完之后，重定向到另一个页面，防止重刷
        return "redirect:http://cart.gulimall.com/addSuccessPage.html";
    }

    /**
     * 商品添加成功页
     *
     * @param skuId
     * @param model
     * @return
     */
    @GetMapping("/addSuccessPage.html")
    public String addToCartSuccessPage(@RequestParam("skuId") Long skuId, Model model) {
        CartItemVo cartItemVo = cartService.getCartItem(skuId);
        model.addAttribute("item", cartItemVo);
        return "success";
    }

    /**
     * 获取当前用户的购物车里面的商品
     * @return
     */
    @GetMapping("/currentUserCartItems")
    @ResponseBody
    public List<CartItemVo> getcurrentUserCartItems() {
        List<CartItemVo> cartItemVos = cartService.getcurrentUserCartItems();
        return cartItemVos;
    }

    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId, @RequestParam("checked") Integer checked) {
        cartService.checkItem(skuId, checked);
        return "/redirect:http://cart.gulimall.com/cartList.html";
    }

    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num) {
        cartService.countItem(skuId, num);
        return "/redirect:http://cart.gulimall.com/cartList.html";
    }

    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId) {
        cartService.deleteItem(skuId);
        return "/redirect:http://cart.gulimall.com/cartList.html";
    }
}
