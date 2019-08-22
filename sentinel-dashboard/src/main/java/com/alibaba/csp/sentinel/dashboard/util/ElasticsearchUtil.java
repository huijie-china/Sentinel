package com.alibaba.csp.sentinel.dashboard.util;

import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author : Haifeng.Pang.
 * @version 0.1 : ElasticsearchUtil v0.1 2019-08-08 17:54 By Kevin.
 * @description :
 */
@Component
public class ElasticsearchUtil {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchUtil.class);

    @Resource(name="restHighLevelClient")
    private RestHighLevelClient restHighLevelClient;

    private static RestHighLevelClient client;

    private static ObjectMapper mapper = new ObjectMapper();

    /**
     * @PostContruct是spring框架的注解
     * spring容器初始化的时候执行该方法
     */
    @PostConstruct
    public void init() {
        client = this.restHighLevelClient;
    }

    /**
     * 创建索引
     *
     * @param index
     * @return
     */
    /*public static boolean createIndex(String index) {
        //index名必须全小写，否则报错
        CreateIndexRequest request = new CreateIndexRequest(index);
        try {
            CreateIndexResponse indexResponse = client.index().create(request);
            if (indexResponse.isAcknowledged()) {
                LOGGER.info("创建索引成功");
            } else {
                LOGGER.info("创建索引失败");
            }
            return indexResponse.isAcknowledged();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }*/

    /**
     * 插入数据
     * @param index
     * @param type
     * @param object
     * @return
     */
    public static String addData(String index, String type, String id, Object object) {
        try {
            IndexResponse indexResponse = client.index(buildIndexRequest(index, type, id, object));
            return indexResponse.getId();
        } catch (Exception e) {
            logger.error("增加数据发生异常 :", e);
        }
        return null;
    }

    public static IndexRequest buildIndexRequest(String index, String type, String id, Object object) {
        IndexRequest indexRequest = null;

        if (StringUtil.isBlank(id)) {
            indexRequest = new IndexRequest(index, type);
        }else {
            indexRequest = new IndexRequest(index, type, id);
        }
        return indexRequest.source(JSON.toJSONString(object), XContentType.JSON);
    }

    public static List<String> batchAddData(List<IndexRequest> requests) {

        if (requests == null || requests.size() <= 0) {
            return new ArrayList<>();
        }

        BulkRequest bulkRequest = new BulkRequest();
        for (IndexRequest indexRequest : requests) {
            bulkRequest.add(indexRequest);
        }
        try {
            BulkResponse bulkResponse = client.bulk(bulkRequest);
            return Arrays.stream(bulkResponse.getItems()).map(BulkItemResponse::getId).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("批量增加数据发生异常 :", e);
        }
        return null;
    }

    public static SearchResponse search(SearchRequest searchRequest) {
        try {
            return client.search(searchRequest);
        } catch (IOException e) {
            // TODO Auto-generated catch block
           logger.error("查询 ES 出现异常 :", e);
           return null;
        }
    }

    /**
     * 聚合搜索
     * @param searchRequest
     */
    public static void aggSearch(SearchRequest searchRequest) {
        try {
            SearchResponse response = client.search(searchRequest);
            System.out.println(response);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 检查索引
     * @param index
     * @return
     * @throws IOException
     */
    public static boolean checkIndexExist(String index) {
        try {
            Response response = client.getLowLevelClient().performRequest("HEAD", index);
            boolean exist = response.getStatusLine().getReasonPhrase().equals("OK");
            return exist;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * 获取低水平客户端
     * @return
     */
    public static RestClient getLowLevelClient() {
        return client.getLowLevelClient();
    }
}
