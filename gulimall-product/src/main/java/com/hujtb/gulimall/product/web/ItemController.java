package com.hujtb.gulimall.product.web;

import com.hujtb.gulimall.product.service.SkuInfoService;
import com.hujtb.gulimall.product.vo.SkuItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ItemController {

    @Autowired
    SkuInfoService skuInfoService;

    /**
     * 查询商品详情信息
     * @param skuId
     * @param model
     * @return
     */
    @GetMapping("/{skuId}.html")
    public String skuItem(@PathVariable("skuId") Long skuId, Model model) {
        SkuItemVo itemVo = skuInfoService.item(skuId);
        model.addAttribute("item", itemVo);
        return "item";
    }
}
