package com.hujtb.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.hujtb.common.to.es.SkuEsModel;
import com.hujtb.gulimall.search.config.GulimallElasticConfig;
import com.hujtb.gulimall.search.constant.EsConstant;
import com.hujtb.gulimall.search.service.MallSearchService;
import com.hujtb.gulimall.search.vo.SearchParamVo;
import com.hujtb.gulimall.search.vo.SearchResponseVo;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MallSearchServiceImpl implements MallSearchService {

    @Autowired
    RestHighLevelClient client;

    @Override
    public SearchResponseVo search(SearchParamVo param) {
        SearchResponseVo responseVo = null;
        // 准备检索请求
        SearchRequest searchRequest = buildSearchRequest(param);
        SearchResponse response = null;
        // 执行检索请求
        try {
            response = client.search(searchRequest, GulimallElasticConfig.COMMON_OPTIONS);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 分析检索请求，将SearchResponse封装成SearchResponseVo
        responseVo = buildSearchResponseVo(response, param);
        return responseVo;
    }

    /**
     * 构建请求，动态构建DSL语句
     *
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParamVo param) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        /**
         * 模糊匹配，过滤（按照属性、分类、品牌、价格区间、库存）
         */
        // 构建bool - query
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        // 1.1 must
        if (StringUtils.isNotEmpty(param.getKeyword())) {
            boolQueryBuilder.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
        }

        // 1.2.1 filter - 按照三级分类查询
        if (param.getCatalog3Id() != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("catalogId", param.getCatalog3Id()));
        }

        // 1.2.2 filter - 按照品牌id查询
        if (param.getBrandId() != null && param.getBrandId().size() > 0) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", param.getBrandId()));
        }
        // 1.2.3 filter - 按照属性查询
        if (param.getAttrs() != null && param.getAttrs().size() > 0) {
            for (String attr : param.getAttrs()) {
                BoolQueryBuilder attrBoolQueryBuilder = QueryBuilders.boolQuery();
                String[] s = attr.split("_");
                String attrId = s[0];
                String[] values = s[1].split(":");
                attrBoolQueryBuilder.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                attrBoolQueryBuilder.must(QueryBuilders.termQuery("attrs.attrValue", values));
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", null, ScoreMode.None);
                boolQueryBuilder.filter(nestedQuery);
            }
        }

        // 1.2.4 filter - 按照是否有库存查询
        boolQueryBuilder.filter(QueryBuilders.termQuery("hasStock", param.getHasStock() == 1));

        // 1.2.5 filter - 按照价格区间查询
        if (StringUtils.isNotEmpty(param.getSkuPrice())) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            String[] s = param.getSkuPrice().split("_");
            if (s.length == 2) {
                rangeQuery.gte(s[0]).lte(s[1]);
            } else if (s.length == 1) {
                if (s[0] == "") {
                    rangeQuery.lte(s[1]);
                } else {
                    rangeQuery.gte(s[0]);
                }
            }
            boolQueryBuilder.filter(rangeQuery);
        }
        searchSourceBuilder.query(boolQueryBuilder);

        /**
         * 排序、分页、高亮
         */
        // 2.1 排序
        if (StringUtils.isNotEmpty(param.getSort())) {
            String sort = param.getSort();
            String[] s = sort.split("_");
            SortOrder sortOrder = s[1].equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC;
            searchSourceBuilder.sort(s[0], sortOrder);
        }

        // 2.2 分页
        Integer pageNum = param.getPageNum();
        searchSourceBuilder.from((pageNum - 1) * EsConstant.PRODUCT_PAGESIZE);
        searchSourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);

        // 2.3 高亮
        if (StringUtils.isNotEmpty(param.getKeyword())) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuTitle").preTags("<b style='color:red'>").postTags("</b>");
            searchSourceBuilder.highlighter(highlightBuilder);
        }

        /**
         * 聚合分析
         */
        // 3.1 品牌聚合
        TermsAggregationBuilder brandAgg = AggregationBuilders.terms("brand_agg");
        brandAgg.field("brandId").size(50);
        // 品牌聚合子聚合
        brandAgg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(50));
        brandAgg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(50));
        searchSourceBuilder.aggregation(brandAgg);

        // 3.2 分类聚合
        TermsAggregationBuilder catalogAgg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(2);
        // 分类聚合子聚合
        catalogAgg.subAggregation(AggregationBuilders.terms("catalogName").field("catalogName").size(1));
        searchSourceBuilder.aggregation(catalogAgg);

        // 3.3 属性聚合
        NestedAggregationBuilder attrsAgg = AggregationBuilders.nested("attrs_agg", "attrs");
        // 聚合出当前所有的attrId
        TermsAggregationBuilder attrIdAgg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
        // 聚合分析出当前attrId对应的名字
        attrIdAgg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        attrIdAgg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(1));
        attrsAgg.subAggregation(attrIdAgg);
        searchSourceBuilder.aggregation(attrsAgg);


        System.out.println("构建的DSL：" + searchSourceBuilder.toString());
        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, searchSourceBuilder);
        return searchRequest;
    }

    /**
     * 构建结果数据
     *
     * @param response
     * @return
     */
    private SearchResponseVo buildSearchResponseVo(SearchResponse response, SearchParamVo param) {
        SearchResponseVo responseVo = new SearchResponseVo();

        // 返回所有查询到的商品
        SearchHits hits = response.getHits();
        List<SkuEsModel> skuEsModelList = new ArrayList<>();
        if (hits.getHits() != null && hits.getHits().length > 0) {
            SearchHit[] hits1 = hits.getHits();
            for (SearchHit hit : hits1) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel esModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
                // 如果传递了keyword，skuTitle属性的值不单单是文本，样式也要设置进去
                if (StringUtils.isNotEmpty(param.getKeyword())) {
                    String highlightSkuTitle = hit.getHighlightFields().get("skuTitle").getFragments()[0].toString();
                    esModel.setSkuTitle(highlightSkuTitle);
                }
                skuEsModelList.add(esModel);
            }
        }
        responseVo.setProducts(skuEsModelList);

        // 当前所有商品涉及的所有属性信息
        ParsedNested attrsAgg = response.getAggregations().get("attrs_agg");
        ParsedLongTerms attrsIdAgg = attrsAgg.getAggregations().get("attrs_id_agg");
        List<SearchResponseVo.AttrVo> attrVos = new ArrayList<>();
        for (Terms.Bucket bucket : attrsIdAgg.getBuckets()) {
            SearchResponseVo.AttrVo attrVo = new SearchResponseVo.AttrVo();
            Long attrId = bucket.getKeyAsNumber().longValue();
            String attrName = ((ParsedStringTerms) bucket.getAggregations().get("attr_name_agg")).getBuckets().get(0).getKeyAsString();
            List<String> attrValue = ((ParsedStringTerms) bucket.getAggregations().get("attr_value_agg")).getBuckets().stream().map(item -> {
                String keyAsString = ((Terms.Bucket) item).getKeyAsString();
                return keyAsString;
            }).collect(Collectors.toList());

            attrVo.setAttrId(attrId);
            attrVo.setAttrName(attrName);
            attrVo.setAttrValue(attrValue);
            attrVos.add(attrVo);
        }
        responseVo.setAttrs(attrVos);

        // 当前所有商品涉及的所有品牌信息
        List<SearchResponseVo.BrandVo> brandVos = new ArrayList<>();
        ParsedLongTerms brandAgg = response.getAggregations().get("brand_agg");
        List<? extends Terms.Bucket> brandAggBuckets = brandAgg.getBuckets();
        for (Terms.Bucket bucket : brandAggBuckets) {
            SearchResponseVo.BrandVo brandVo = new SearchResponseVo.BrandVo();
            ParsedStringTerms brandNameAgg = bucket.getAggregations().get("brand_name_agg");
            String brandName = brandNameAgg.getBuckets().get(0).getKeyAsString();
            ParsedStringTerms brandImgAgg = bucket.getAggregations().get("brand_img_agg");
            String brandImg = brandImgAgg.getBuckets().get(0).getKeyAsString();

            brandVo.setBrandId(Long.parseLong(bucket.getKeyAsString()));
            brandVo.setBrandName(brandName);
            brandVo.setBrandImg(brandImg);
            brandVos.add(brandVo);
        }
        responseVo.setBrands(brandVos);

        // 当前所有商品涉及的所有分类信息
        ParsedLongTerms catalogAgg = response.getAggregations().get("catalog_agg");
        List<SearchResponseVo.CatalogVo> catalogVos = new ArrayList<>();
        List<? extends Terms.Bucket> catalogAggBuckets = catalogAgg.getBuckets();
        for (Terms.Bucket bucket : catalogAggBuckets) {
            SearchResponseVo.CatalogVo catalogVo = new SearchResponseVo.CatalogVo();
            catalogVo.setCatalogId(Long.parseLong(bucket.getKeyAsString()));
            ParsedStringTerms catalogNameAgg = bucket.getAggregations().get("catalog_name_agg");
            String catalogName = catalogNameAgg.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(catalogName);
            catalogVos.add(catalogVo);
        }
        responseVo.setCatalogs(catalogVos);
        // ========以上信息从聚合中取========

        // 分页信息 - 页码
        responseVo.setPageNum(param.getPageNum());
        // 分页信息 - 总记录数
        Long total = hits.getTotalHits().value;
        responseVo.setTotal(total);
        // 分页信息 - 总页码
        Long totalPages = total % EsConstant.PRODUCT_PAGESIZE == 0 ? total / EsConstant.PRODUCT_PAGESIZE : (total % EsConstant.PRODUCT_PAGESIZE + 1);
        responseVo.setTotalPages(totalPages);
        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            pageNavs.add(i);
        }
        responseVo.setPageNavs(pageNavs);
        return responseVo;
    }
}

