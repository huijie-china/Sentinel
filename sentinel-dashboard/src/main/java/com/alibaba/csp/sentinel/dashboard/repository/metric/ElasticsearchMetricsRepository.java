package com.alibaba.csp.sentinel.dashboard.repository.metric;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.MetricEntity;
import com.alibaba.csp.sentinel.dashboard.util.ElasticsearchUtil;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author : Haifeng.Pang.
 * @version 0.1 : ElasticsearchMetricsRepository v0.1 2019-08-08 17:28 By Kevin.
 * @description :
 */
@Repository("elasticsearchMetricsRepository")
public class ElasticsearchMetricsRepository implements MetricsRepository<MetricEntity> {

    /**时间格式*/
    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private static final String SENTINEL_INDEX = "sentinel_metric";

    /**
     * Save the metric to the storage repository.
     *
     * @param metric metric data to save
     */
    @Override
    public void save(MetricEntity metric) {
        if (metric == null || StringUtil.isBlank(metric.getApp())) {
            return;
        }

        ElasticsearchUtil.addData(SENTINEL_INDEX, "doc", null, metric);
    }

    /**
     * Save all metrics to the storage repository.
     *
     * @param metrics metrics to save
     */
    @Override
    public void saveAll(Iterable<MetricEntity> metrics) {
        if (metrics == null) {
            return;
        }
        List<IndexRequest> requests = new ArrayList<>();

        metrics.forEach(item -> {
            requests.add(ElasticsearchUtil.buildIndexRequest(SENTINEL_INDEX, "doc", null, item));
        });
        ElasticsearchUtil.batchAddData(requests);
    }

    /**
     * Get all metrics by {@code appName} and {@code resourceName} between a period of time.
     *
     * @param app       application name for Sentinel
     * @param resource  resource name
     * @param startTime start timestamp
     * @param endTime   end timestamp
     * @return all metrics in query conditions
     */
    @Override
    public List<MetricEntity> queryByAppAndResourceBetween(String app, String resource, long startTime, long endTime) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("app", app);
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("timestamp").gte(startTime).lte(endTime);
        BoolQueryBuilder boolBuilder = QueryBuilders.boolQuery();
        boolBuilder.must(matchQueryBuilder);
        boolBuilder.must(rangeQueryBuilder);
        sourceBuilder.query(boolBuilder);
        SearchRequest searchRequest = new SearchRequest(SENTINEL_INDEX);
        searchRequest.types("doc");
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse = ElasticsearchUtil.search(searchRequest);
        SearchHits searchHits = searchResponse.getHits();

        List<MetricEntity> metricEntityList = new ArrayList<>();

        Iterator<SearchHit> searchHitIterator = searchHits.iterator();

        if (!searchHitIterator.hasNext()) {
            return metricEntityList;
        }

        while (searchHitIterator.hasNext()) {
            SearchHit hit = searchHitIterator.next();
            metricEntityList.add(JSON.parseObject(hit.getSourceAsString(), MetricEntity.class));
        }
        return metricEntityList;
    }

    /**
     * List resource name of provided application name.
     *
     * @param app application name
     * @return list of resources
     */
    @Override
    public List<String> listResourcesOfApp(String app) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.size(0);
        //sourceBuilder.fetchSource(new String[] {"resource"}, new String[]{});
        TermsAggregationBuilder aggregation = AggregationBuilders.terms("resourceName")
                .field("resource.keyword").order(BucketOrder.count(false)).size(Integer.MAX_VALUE);

        BoolQueryBuilder boolBuilder = QueryBuilders.boolQuery();

        long endTime = System.currentTimeMillis();
        long startTime = System.currentTimeMillis() - 1000 * 60 * 60;
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("timestamp").gte(startTime).lte(endTime);
        MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("app", app);
        boolBuilder.must(rangeQueryBuilder);
        boolBuilder.must(matchQueryBuilder);
        sourceBuilder.aggregation(aggregation);
        sourceBuilder.query(boolBuilder);
        SearchRequest searchRequest = new SearchRequest(SENTINEL_INDEX);
        searchRequest.types("doc");
        searchRequest.source(sourceBuilder);
        SearchResponse response = ElasticsearchUtil.search(searchRequest);
        if (response == null) {
            return new ArrayList<>();
        }
        Map<String, Aggregation> aggregationMap = response.getAggregations().asMap();
        ParsedStringTerms parsedStringTerms = (ParsedStringTerms)aggregationMap.get("resourceName");
        if (parsedStringTerms == null) {
            return new ArrayList<>();
        }

        List bucketObjList = parsedStringTerms.getBuckets();

        if (bucketObjList == null || bucketObjList.size() <= 0) {
            return new ArrayList<>();
        }

        List<String> resultList = new ArrayList<>();
        for (Object o : bucketObjList) {
            ParsedStringTerms.ParsedBucket obj = (ParsedStringTerms.ParsedBucket) o;
            resultList.add(obj.getKeyAsString());
        }
        return resultList;
    }

    /*private MetricEntity convertToMetricEntity(MetricPO metricPO) {
        MetricEntity metricEntity = new MetricEntity();

        metricEntity.setId(metricPO.getId());
        metricEntity.setGmtCreate(new Date(metricPO.getGmtCreate()));
        metricEntity.setGmtModified(new Date(metricPO.getGmtModified()));
        metricEntity.setApp(metricPO.getApp());
        // 查询数据减8小时
        metricEntity.setTimestamp(Date.from(metricPO.getTime().minusMillis(TimeUnit.HOURS.toMillis(UTC_8))));
        metricEntity.setResource(metricPO.getResource());
        metricEntity.setPassQps(metricPO.getPassQps());
        metricEntity.setSuccessQps(metricPO.getSuccessQps());
        metricEntity.setBlockQps(metricPO.getBlockQps());
        metricEntity.setExceptionQps(metricPO.getExceptionQps());
        metricEntity.setRt(metricPO.getRt());
        metricEntity.setCount(metricPO.getCount());

        return metricEntity;
    }*/
}
