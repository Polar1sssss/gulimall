package com.hujtb.gulimall.search.constant;

public class EsConstant {
    // 去掉映射里面的doc_value，之后用POST_INDEX命令迁移数据
    public static final String PRODUCT_INDEX = "gulimall_product";
    public static final Integer PRODUCT_PAGESIZE = 16;
}
