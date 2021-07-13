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
package com.meituan.dorado.bootstrap.provider;

import com.meituan.dorado.bootstrap.ServiceBootstrap;
import com.meituan.dorado.bootstrap.provider.meta.ProviderStatus;
import com.meituan.dorado.bootstrap.provider.meta.ServerInfo;
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.common.extension.ExtensionLoader;
import com.meituan.dorado.common.util.NetUtil;
import com.meituan.dorado.common.util.VersionUtil;
import com.meituan.dorado.config.service.ProviderConfig;
import com.meituan.dorado.config.service.ServiceConfig;
import com.meituan.dorado.registry.Registry;
import com.meituan.dorado.registry.RegistryFactory;
import com.meituan.dorado.registry.RegistryPolicy;
import com.meituan.dorado.registry.meta.RegistryInfo;
import com.meituan.dorado.transport.Server;
import com.meituan.dorado.transport.ServerFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ServicePublisher extends ServiceBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicePublisher.class);

    private static String appkey;

    private static final ConcurrentMap<Integer, RegistryPolicy> registryPolicyMap = new ConcurrentHashMap<>();

    public static String getAppkey() {
        return appkey;
    }

    public static void publishService(ProviderConfig config) {
        initHttpServer(RpcRole.PROVIDER);

        initAppkey(config.getAppkey());
        ServerFactory serverFactory = ExtensionLoader.getExtension(ServerFactory.class);
        Server server = serverFactory.buildServer(config);

        registerService(config);
        ProviderInfoRepository.addProviderInfo(config, server);
        for (ServiceConfig serviceConfig : config.getServiceConfigList()) {
            serviceConfig.loadAndInitExtensionList(serviceConfig, RpcRole.PROVIDER);
        }
        LOGGER.info("Dorado service published: {}", getServiceNameList(config));
    }

    public static void unpublishService(ProviderConfig config) {
        unregisterService(config);

        int port = config.getPort();
        ServerInfo serverInfo = ProviderInfoRepository.getPortServerInfoMap().get(port);
        if (serverInfo != null && serverInfo.getServer() != null) {
            serverInfo.getServer().close();
        }
        ProviderInfoRepository.removeProviderInfo(config);
        LOGGER.info("Dorado service unpublished: {}", getServiceNameList(config));
    }

    private static void registerService(ProviderConfig config) {
        Map<String, String> registryInfo = parseRegistryCfg(config.getRegistry());
        RegistryFactory registryFactory;
        if (registryInfo.isEmpty()) {
            registryFactory = ExtensionLoader.getExtension(RegistryFactory.class);
        } else {
            registryFactory = ExtensionLoader.getExtensionWithName(RegistryFactory.class, registryInfo.get(Constants.REGISTRY_WAY_KEY));
        }
        RegistryPolicy registry = registryFactory.getRegistryPolicy(registryInfo, Registry.OperType.REGISTRY);

        registry.register(convertProviderCfg2RegistryInfo(config, registry.getAttachments()));
        registryPolicyMap.put(config.getPort(), registry);
    }

    private static void unregisterService(ProviderConfig config) {
        RegistryPolicy registry = registryPolicyMap.remove(config.getPort());
        if (registry != null) {
            registry.unregister(convertProviderCfg2RegistryInfo(config, registry.getAttachments()));
            registry.destroy();
        }
    }

    private static RegistryInfo convertProviderCfg2RegistryInfo(ProviderConfig providerConfig, Map<String, String> attachments) {
        RegistryInfo info = new RegistryInfo();
        info.setServiceNames(getServiceNameList(providerConfig));
        info.setAppkey(providerConfig.getAppkey());
        info.setIp(NetUtil.getLocalHost());
        info.setPort(providerConfig.getPort());
        info.setProtocol(providerConfig.getProtocol());
        info.setSerialize(providerConfig.getSerialize());
        info.setStatus(ProviderStatus.ALIVE.getCode());
        info.setWeight(providerConfig.getWeight());
        info.setWarmUp(providerConfig.getWarmup());
        info.setVersion(VersionUtil.getDoradoVersion());
        info.setAttachments(attachments);
        info.setEnv(providerConfig.getEnv());
        return info;
    }

    private static void initAppkey(String appkeyConfig) {
        if (StringUtils.isBlank(appkeyConfig)) {
            throw new IllegalArgumentException("Appkey can not be null or empty!");
        }

        if (StringUtils.isBlank(appkey)) {
            appkey = appkeyConfig;
        }

        if (!appkey.equals(appkeyConfig)) {
            throw new IllegalArgumentException("Appkey should be same in one server, but there are two appkeys:" + appkey + "," + appkeyConfig);
        }
    }

    private static List<String> getServiceNameList(ProviderConfig providerConfig) {
        List<String> serviceNameList = new ArrayList<>();
        for (ServiceConfig serviceConfig : providerConfig.getServiceConfigList()) {
            serviceNameList.add(serviceConfig.getServiceName());
        }
        return serviceNameList;
    }

}
