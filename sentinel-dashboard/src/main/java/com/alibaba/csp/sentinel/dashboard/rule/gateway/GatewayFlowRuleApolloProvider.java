package com.alibaba.csp.sentinel.dashboard.rule.gateway;

import com.alibaba.csp.sentinel.dashboard.apollo.ApolloConfigUtil;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.gateway.GatewayFlowRuleEntity;
import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.ctrip.framework.apollo.openapi.client.ApolloOpenApiClient;
import com.ctrip.framework.apollo.openapi.dto.OpenItemDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenNamespaceDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : Haifeng.Pang.
 * @version 0.1 : GatewayFlowRuleApolloProvider v0.1 2019-08-06 17:20 By Kevin.
 * @description :
 */
@Component("gatewayFlowRuleApolloProvider")
public class GatewayFlowRuleApolloProvider implements DynamicGatewayRuleProvider<List<GatewayFlowRuleEntity>> {

    @Value("${apollo.env}")
    private String env;

    @Value("${apollo.clusterName}")
    private String clusterName;

    @Value("${apollo.namespaceName.flowRule}")
    private String namespaceNameFlowRule;

    @Autowired
    private ApolloOpenApiClient apolloOpenApiClient;
    @Autowired
    private Converter<String, List<GatewayFlowRuleEntity>> converter;

    @Override
    public List<GatewayFlowRuleEntity>  getRules(String appName, String ip, String port) throws Exception {
        String flowDataId = ApolloConfigUtil.getFlowDataId(appName);
        OpenNamespaceDTO openNamespaceDTO = apolloOpenApiClient.getNamespace(appName, env, clusterName, namespaceNameFlowRule);
        String rules = openNamespaceDTO
                .getItems()
                .stream()
                .filter(p -> p.getKey().equals(flowDataId))
                .map(OpenItemDTO::getValue)
                .findFirst()
                .orElse("");

        if (StringUtil.isEmpty(rules)) {
            return new ArrayList<>();
        }
        return converter.convert(rules);
    }
}
