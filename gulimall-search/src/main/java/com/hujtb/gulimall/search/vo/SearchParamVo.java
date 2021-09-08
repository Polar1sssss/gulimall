package com.hujtb.gulimall.search.vo;

import lombok.Data;

import java.util.List;

@Data
public class SearchParamVo {

    // 页面传递过来的全文匹配关键字
    private String keyword;

    // 三级分类id
    private Long catalog3Id;

    /** 排序条件
     * sort=saleCount_asc/desc
     * sort=hotScore_asc/desc
     * sort=skuPrice_asc/desc
     */
    private String sort;

    /**
     * 过滤条件
     * hasStock 是否有货：hasStock=0/1
     * skuPrice 价格区间：skuPrice=0_500 skuPrice=_500 skuPrice=500_
     * brandId 品牌：brandId=2&brandId=3
     * attrs 各种属性：attrs=1_安卓:ios&attrs=3_5寸:6寸
     *
     */
    private Integer hasStock = 1;
    private String skuPrice;
    private List<Long> brandId;
    private List<String> attrs;
    private Integer pageNum;
}
