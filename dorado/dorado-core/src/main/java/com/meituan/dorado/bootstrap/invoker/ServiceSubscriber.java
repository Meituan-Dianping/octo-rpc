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
package com.meituan.dorado.bootstrap.invoker;

import com.meituan.dorado.bootstrap.ServiceBootstrap;
import com.meituan.dorado.cluster.Cluster;
import com.meituan.dorado.cluster.ClusterHandler;
import com.meituan.dorado.cluster.InvokerRepository;
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.common.exception.RpcException;
import com.meituan.dorado.common.extension.ExtensionLoader;
import com.meituan.dorado.config.service.ClientConfig;
import com.meituan.dorado.config.service.ReferenceConfig;
import com.meituan.dorado.registry.Registry;
import com.meituan.dorado.registry.RegistryFactory;
import com.meituan.dorado.registry.RegistryPolicy;
import com.meituan.dorado.registry.meta.SubscribeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ServiceSubscriber extends ServiceBootstrap {

    private static final Logger logger = LoggerFactory.getLogger(ServiceSubscriber.class);

    public static ClusterHandler subscribeService(ReferenceConfig config) {
        initHttpServer(RpcRole.INVOKER);

        ClientConfig clientConfig = genClientConfig(config);
        InvokerRepository repository;
        if (isDirectConn(config)) {
            repository = new InvokerRepository(clientConfig, true);
        } else {
            repository = new InvokerRepository(clientConfig);
            doSubscribeService(config, repository);
        }
        ClusterHandler clusterHandler = getClusterHandler(config.getClusterPolicy(), repository);
        return clusterHandler;
    }

    private static void doSubscribeService(ReferenceConfig cfg, InvokerRepository repository) {
        Map<String, String> registryInfo = parseRegistryCfg(cfg.getRegistry());
        RegistryFactory registryFactory;
        if (registryInfo.isEmpty()) {
            registryFactory = ExtensionLoader.getExtension(RegistryFactory.class);
        } else {
            registryFactory = ExtensionLoader.getExtensionWithName(RegistryFactory.class, registryInfo.get(Constants.REGISTRY_WAY_KEY));
        }
        RegistryPolicy registry = registryFactory.getRegistryPolicy(registryInfo, Registry.OperType.DISCOVERY);
        SubscribeInfo info = convertReferenceCfg2SubscribeInfo(cfg, registry.getAttachments());
        repository.setSubscribeInfo(info);
        repository.setRegistry(registry);

        registry.subcribe(info, repository);
    }

    private static ClusterHandler getClusterHandler(String clusterPolicy, InvokerRepository repository) {
        Cluster cluster = ExtensionLoader.getExtensionWithName(Cluster.class, clusterPolicy);
        if (cluster == null) {
            logger.warn("Not find cluster by policy {}, change to {}", clusterPolicy, Constants.DEFAULT_CLUSTER_POLICY);
            clusterPolicy = Constants.DEFAULT_CLUSTER_POLICY;
            cluster = ExtensionLoader.getExtensionWithName(Cluster.class, clusterPolicy);
        }
        if (cluster == null) {
            throw new RpcException("Not find cluster by policy " + clusterPolicy);
        }

        ClusterHandler clusterHandler = cluster.buildClusterHandler(repository);
        if (clusterHandler == null) {
            throw new RpcException("Not find cluster Handler by policy " + clusterPolicy);
        }
        return clusterHandler;
    }

    private static ClientConfig genClientConfig(ReferenceConfig config) {
        return new ClientConfig(config);
    }

    private static boolean isDirectConn(ReferenceConfig config) {
        return config.getDirectConnAddress() != null;
    }

    private static SubscribeInfo convertReferenceCfg2SubscribeInfo(ReferenceConfig cfg,
                                                                   Map<String, String> attachments) {
        SubscribeInfo info = new SubscribeInfo();
        info.setServiceName(cfg.getServiceName());
        info.setLocalAppkey(cfg.getAppkey());
        info.setRemoteAppkey(cfg.getRemoteAppkey());
        info.setProtocol(cfg.getProtocol());
        info.setSerialize(cfg.getSerialize());
        info.setEnv(cfg.getEnv());
        info.setAttachments(attachments);
        return info;
    }
}
