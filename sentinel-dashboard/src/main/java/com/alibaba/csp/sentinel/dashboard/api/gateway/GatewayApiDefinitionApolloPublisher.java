/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.dashboard.api.gateway;

import com.alibaba.csp.sentinel.dashboard.apollo.ApolloConfigUtil;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.gateway.ApiDefinitionEntity;
import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.util.AssertUtil;
import com.ctrip.framework.apollo.openapi.client.ApolloOpenApiClient;
import com.ctrip.framework.apollo.openapi.dto.NamespaceReleaseDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenItemDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Eric Zhao
 * @since 1.4.0
 */
@Component("gatewayApiDefinitionApolloPublisher")
public class GatewayApiDefinitionApolloPublisher implements DynamicGatewayApiDefinitionPublisher<List<ApiDefinitionEntity>> {

    @Value("${apollo.env}")
    private String env;

    @Value("${apollo.clusterName}")
    private String clusterName;

    @Value("${apollo.namespaceName.apiDefinition}")
    private String namespaceNameApiDefinition;

    @Autowired
    private ApolloOpenApiClient apolloOpenApiClient;
    @Autowired
    private Converter<List<ApiDefinitionEntity>, String> converter;

    @Override
    public void publish(String appName, String ip, String port, List<ApiDefinitionEntity> apiDefinitions) throws Exception {
        AssertUtil.notEmpty(appName, "app name cannot be empty");
        if (apiDefinitions == null) {
            return;
        }
        // Increase the configuration
        String appId = appName;
        String apiDefinitionDataId = ApolloConfigUtil.getApiDefinitionDataId(appName);
        OpenItemDTO openItemDTO = new OpenItemDTO();
        openItemDTO.setKey(apiDefinitionDataId);
        openItemDTO.setValue(converter.convert(apiDefinitions));
        openItemDTO.setComment("Program auto-join");
        openItemDTO.setDataChangeCreatedBy("admin");
        apolloOpenApiClient.createOrUpdateItem(appId, env, clusterName, namespaceNameApiDefinition, openItemDTO);

        // Release configuration
        NamespaceReleaseDTO namespaceReleaseDTO = new NamespaceReleaseDTO();
        namespaceReleaseDTO.setEmergencyPublish(true);
        namespaceReleaseDTO.setReleaseComment("Modify or add configurations");
        namespaceReleaseDTO.setReleasedBy("admin");
        namespaceReleaseDTO.setReleaseTitle("Modify or add configurations");
        apolloOpenApiClient.publishNamespace(appId, env, clusterName, namespaceNameApiDefinition, namespaceReleaseDTO);
    }
}
