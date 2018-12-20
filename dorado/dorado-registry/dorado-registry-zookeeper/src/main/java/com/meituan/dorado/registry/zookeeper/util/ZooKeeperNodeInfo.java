/*
 * Copyright 2018 Meituan Dianping. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.meituan.dorado.registry.zookeeper.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meituan.dorado.bootstrap.provider.ProviderStatus;
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.registry.meta.Provider;
import com.meituan.dorado.registry.meta.RegistryInfo;
import com.meituan.dorado.registry.meta.SubscribeInfo;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 注册路径：/octo/nameservice/{env}/{appkey}/provider/{ip:port}
 */
public class ZooKeeperNodeInfo {

    public static final String ROOT_NAME = "octo/nameservice";
    public static final String PATH_SEPARATOR = "/";
    public static final String PROVIDER = "provider";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public static String genNodeData(RegistryInfo info) throws JsonProcessingException {
        ChildNodeData nodeData = new ChildNodeData();
        nodeData.setAppkey(info.getAppkey());
        nodeData.setIp(info.getIp());
        nodeData.setPort(info.getPort());

        Map<String, ServiceDetail> serviceDetailMap = genServiceDetailMap(info.getServiceNames());
        nodeData.setServiceInfo(serviceDetailMap);

        nodeData.setProtocol(info.getProtocol());
        nodeData.setFweight(info.getWeight());
        nodeData.setWeight((int) info.getWeight());
        nodeData.setWarmup(info.getWarmUp());
        nodeData.setEnv(Constants.EnvType.getEnvType(info.getEnv()).getEnvCode());
        nodeData.setVersion(info.getVersion());
        if (ProviderStatus.isNotAliveStatus(info.getStatus())) {
            nodeData.setStatus(info.getStatus());
        } else {
            nodeData.setStatus(ProviderStatus.ALIVE.getCode());
        }
        nodeData.setLastUpdateTime(System.currentTimeMillis() / 1000);
        String nodeDataStr = objectMapper.writeValueAsString(nodeData);
        return nodeDataStr;
    }

    public static Provider genProvider(String nodeDataStr, SubscribeInfo info) throws IOException {
        ChildNodeData nodeData = objectMapper.readValue(nodeDataStr, ChildNodeData.class);
        if (!checkNodeData(nodeData, info.getProtocol())) {
            return null;
        }

        Map<String, ServiceDetail> serviceDetailMap = nodeData.getServiceInfo();
        if (serviceDetailMap != null && !serviceDetailMap.isEmpty() &&
                !serviceDetailMap.containsKey(info.getServiceName())) {
            return null;
        }
        boolean isOctoProtocol = false;
        if (serviceDetailMap != null && serviceDetailMap.containsKey(info.getServiceName())) {
            // 仅针对有serviceInfo数据的服务校验
            ServiceDetail svcDetail = serviceDetailMap.get(info.getServiceName());
            if (svcDetail != null && svcDetail.getUnifiedProto() == 1) {
                isOctoProtocol = true;
            }
        }

        Provider provider = new Provider();
        provider.setAppkey(nodeData.getAppkey());
        provider.setProtocol(info.getProtocol());
        provider.setOctoProtocol(isOctoProtocol);
        provider.setIp(nodeData.getIp());
        provider.setPort(nodeData.getPort());
        provider.setStartTime(nodeData.getLastUpdateTime());
        provider.setStatus(nodeData.getStatus());
        provider.setVersion(nodeData.getVersion());
        provider.setWeight(nodeData.getFweight());
        provider.setWarmUp(nodeData.getWarmup());
        provider.setEnv(Constants.EnvType.getEnvName(nodeData.getEnv()));
        return provider;
    }

    private static boolean checkNodeData(ChildNodeData nodeData, String clientProtocol) {
        if (StringUtils.isNotBlank(nodeData.getProtocol()) &&
                !nodeData.getProtocol().equalsIgnoreCase(clientProtocol)) {
            return false;
        }
        if (StringUtils.isBlank(nodeData.getIp())) {
            return false;
        }
        if (nodeData.getPort() <= 0) {
            return false;
        }
        if (nodeData.getStatus() != ProviderStatus.ALIVE.getCode()) {
            return false;
        }
        return true;
    }

    private static Map<String, ServiceDetail> genServiceDetailMap(List<String> serviceNames) {
        Map<String, ServiceDetail> serviceDetailMap = new HashMap<>();
        if (serviceNames == null || serviceNames.isEmpty()) {
            return serviceDetailMap;
        }
        for (String serviceName : serviceNames) {
            serviceDetailMap.put(serviceName, new ServiceDetail(1));
        }
        return serviceDetailMap;
    }
}
