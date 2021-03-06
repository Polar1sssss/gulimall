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
        // ??????????????????
        SearchRequest searchRequest = buildSearchRequest(param);
        SearchResponse response = null;
        // ??????????????????
        try {
            response = client.search(searchRequest, GulimallElasticConfig.COMMON_OPTIONS);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // ????????????????????????SearchResponse?????????SearchResponseVo
        responseVo = buildSearchResponseVo(response, param);
        return responseVo;
    }

    /**
     * ???????????????????????????DSL??????
     *
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParamVo param) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        /**
         * ?????????????????????????????????????????????????????????????????????????????????
         */
        // ??????bool - query
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        // 1.1 must
        if (StringUtils.isNotEmpty(param.getKeyword())) {
            boolQueryBuilder.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
        }

        // 1.2.1 filter - ????????????????????????
        if (param.getCatalog3Id() != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("catalogId", param.getCatalog3Id()));
        }

        // 1.2.2 filter - ????????????id??????
        if (param.getBrandId() != null && param.getBrandId().size() > 0) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", param.getBrandId()));
        }
        // 1.2.3 filter - ??????????????????
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

        // 1.2.4 filter - ???????????????????????????
        boolQueryBuilder.filter(QueryBuilders.termQuery("hasStock", param.getHasStock() == 1));

        // 1.2.5 filter - ????????????????????????
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
         * ????????????????????????
         */
        // 2.1 ??????
        if (StringUtils.isNotEmpty(param.getSort())) {
            String sort = param.getSort();
            String[] s = sort.split("_");
            SortOrder sortOrder = s[1].equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC;
            searchSourceBuilder.sort(s[0], sortOrder);
        }

        // 2.2 ??????
        Integer pageNum = param.getPageNum();
        searchSourceBuilder.from((pageNum - 1) * EsConstant.PRODUCT_PAGESIZE);
        searchSourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);

        // 2.3 ??????
        if (StringUtils.isNotEmpty(param.getKeyword())) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuTitle").preTags("<b style='color:red'>").postTags("</b>");
            searchSourceBuilder.highlighter(highlightBuilder);
        }

        /**
         * ????????????
         */
        // 3.1 ????????????
        TermsAggregationBuilder brandAgg = AggregationBuilders.terms("brand_agg");
        brandAgg.field("brandId").size(50);
        // ?????????????????????
        brandAgg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(50));
        brandAgg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(50));
        searchSourceBuilder.aggregation(brandAgg);

        // 3.2 ????????????
        TermsAggregationBuilder catalogAgg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(2);
        // ?????????????????????
        catalogAgg.subAggregation(AggregationBuilders.terms("catalogName").field("catalogName").size(1));
        searchSourceBuilder.aggregation(catalogAgg);

        // 3.3 ????????????
        NestedAggregationBuilder attrsAgg = AggregationBuilders.nested("attrs_agg", "attrs");
        // ????????????????????????attrId
        TermsAggregationBuilder attrIdAgg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
        // ?????????????????????attrId???????????????
        attrIdAgg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        attrIdAgg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(1));
        attrsAgg.subAggregation(attrIdAgg);
        searchSourceBuilder.aggregation(attrsAgg);


        System.out.println("?????????DSL???" + searchSourceBuilder.toString());
        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, searchSourceBuilder);
        return searchRequest;
    }

    /**
     * ??????????????????
     *
     * @param response
     * @return
     */
    private SearchResponseVo buildSearchResponseVo(SearchResponse response, SearchParamVo param) {
        SearchResponseVo responseVo = new SearchResponseVo();

        // ??????????????????????????????
        SearchHits hits = response.getHits();
        List<SkuEsModel> skuEsModelList = new ArrayList<>();
        if (hits.getHits() != null && hits.getHits().length > 0) {
            SearchHit[] hits1 = hits.getHits();
            for (SearchHit hit : hits1) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel esModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
                // ???????????????keyword???skuTitle?????????????????????????????????????????????????????????
                if (StringUtils.isNotEmpty(param.getKeyword())) {
                    String highlightSkuTitle = hit.getHighlightFields().get("skuTitle").getFragments()[0].toString();
                    esModel.setSkuTitle(highlightSkuTitle);
                }
                skuEsModelList.add(esModel);
            }
        }
        responseVo.setProducts(skuEsModelList);

        // ?????????????????????????????????????????????
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

        // ?????????????????????????????????????????????
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

        // ?????????????????????????????????????????????
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
        // ========???????????????????????????========

        // ???????????? - ??????
        responseVo.setPageNum(param.getPageNum());
        // ???????????? - ????????????
        Long total = hits.getTotalHits().value;
        responseVo.setTotal(total);
        // ???????????? - ?????????
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

