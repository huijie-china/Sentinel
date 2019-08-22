package com.alibaba.csp.sentinel.dashboard.api.gateway;

/**
 * @author : Haifeng.Pang.
 * @version 0.1 : DynamicGatewayRulePublisher v0.1 2019-08-06 17:22 By Kevin.
 * @description :
 */
public interface DynamicGatewayApiDefinitionPublisher<T> {

    /**
     * 网关API发布
     * @param app
     * @param apiDefinitions
     * @throws Exception
     */
    void publish(String appName, String ip, String port, T apiDefinitions) throws Exception;
}
