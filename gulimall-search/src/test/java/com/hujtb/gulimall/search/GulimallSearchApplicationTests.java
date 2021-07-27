package com.hujtb.gulimall.search;

import com.alibaba.fastjson.JSON;
import com.hujtb.gulimall.search.config.GulimallElasticConfig;
import lombok.Data;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.naming.directory.SearchResult;
import java.io.IOException;
import java.util.Map;

@SpringBootTest
public class GulimallSearchApplicationTests {

    @Autowired
    private RestHighLevelClient client;

    @Test
    public void searchData() throws IOException {
        // 创建检索请求
        SearchRequest req = new SearchRequest();
        // 指定索引
        req.indices("bank");
        // 指定DSL，检索条件
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchQuery("address", "mill"));
        searchSourceBuilder.aggregation(AggregationBuilders.terms("ageAgg").field("age").size(10));
        searchSourceBuilder.aggregation(AggregationBuilders.avg("balanceAvg").field("balance"));
        req.source(searchSourceBuilder);
        // 执行检索操作
        SearchResponse res = client.search(req, GulimallElasticConfig.COMMON_OPTIONS);

        // 分析检索结果
        // 外边最大的hits
        SearchHits hits = res.getHits();
        SearchHit[] hits1 = hits.getHits();
        for (SearchHit hit : hits1) {
            String id = hit.getId();
            String sourceAsString = hit.getSourceAsString();
        }

        // 检索聚合结果
        Aggregations aggregations = res.getAggregations();
        Terms ageAgg = aggregations.get("ageAgg");
        for (Terms.Bucket bucket : ageAgg.getBuckets()) {
            String keyAsString = bucket.getKeyAsString();
            System.out.println("年龄：" + keyAsString + "有" + bucket.getDocCount() + "个人");
        }
        Avg balanceAvg = aggregations.get("balanceAvg");
        double balance = balanceAvg.getValue();
        System.out.println(balance);
    }

    /**
     * 存储数据到es
     */
    @Test
    public void indexData() throws IOException {
        IndexRequest indexRequest = new IndexRequest("users");
        indexRequest.id("1");
        // indexRequest.source("userNama", "zhangsan", "age", 18);
        User user = new User();
        user.setAge(123);
        user.setGender("F");
        user.setName("zhangsan");
        String jsonString = JSON.toJSONString(user);
        indexRequest.source(jsonString, XContentType.JSON);

        client.index(indexRequest, GulimallElasticConfig.COMMON_OPTIONS);
    }

    @Data
    class User {
        private String name;
        private String gender;
        private Integer age;
    }

    @Test
    public void contextLoads() {
        System.out.println(client);
    }

}
