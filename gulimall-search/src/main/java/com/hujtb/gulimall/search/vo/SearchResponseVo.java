package com.hujtb.gulimall.search.vo;

import com.hujtb.common.to.es.SkuEsModel;
import lombok.Data;

import java.util.List;

@Data
public class SearchResponseVo {

    private List<SkuEsModel> products;

    /**
     * 分页信息
     */
    private Integer pageNum;
    private Long total;
    private Long totalPages;
    private List<Integer> pageNavs;

    // 当前查询到的结果涉及到的所有品牌
    private List<BrandVo> brands;

    // 当前查询到的商品涉及到的所有属性
    private List<AttrVo> attrs;

    private List<CatalogVo> catalogs;

    @Data
    public static class BrandVo {
        private Long brandId;
        private String brandName;
        private String brandImg;
    }

    @Data
    public static class CatalogVo {
        private Long catalogId;
        private String catalogName;
    }

    @Data
    public static class AttrVo {
        private Long attrId;
        private String attrName;
        private List<String> attrValue;
    }

}
