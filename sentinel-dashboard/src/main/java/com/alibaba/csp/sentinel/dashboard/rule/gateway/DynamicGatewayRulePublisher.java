package com.alibaba.csp.sentinel.dashboard.rule.gateway;

/**
 * @author : Haifeng.Pang.
 * @version 0.1 : DynamicGatewayRulePublisher v0.1 2019-08-06 17:22 By Kevin.
 * @description :
 */
public interface DynamicGatewayRulePublisher<T> {

    /**
     * 网关限流规则发布
     * @param app
     * @param rules
     * @throws Exception
     */
    void publish(String app, T rules) throws Exception;
}
