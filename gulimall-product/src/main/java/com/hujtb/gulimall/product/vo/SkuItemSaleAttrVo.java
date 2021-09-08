package com.hujtb.gulimall.product.vo;

import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * 销售属性
 */
@ToString
@Data
public class SkuItemSaleAttrVo {
    private Long attrId;
    private String attrName;
    private List<AttrValueWithSkuIdVo> attrValues;
}
