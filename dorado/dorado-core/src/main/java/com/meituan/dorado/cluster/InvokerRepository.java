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
package com.meituan.dorado.cluster;

import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.exception.RpcException;
import com.meituan.dorado.common.extension.ExtensionLoader;
import com.meituan.dorado.config.service.ClientConfig;
import com.meituan.dorado.registry.ProviderListener;
import com.meituan.dorado.registry.RegistryPolicy;
import com.meituan.dorado.registry.meta.Provider;
import com.meituan.dorado.registry.meta.SubscribeInfo;
import com.meituan.dorado.rpc.handler.invoker.Invoker;
import com.meituan.dorado.rpc.handler.invoker.InvokerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InvokerRepository<T> implements ProviderListener {

    private static final Logger logger = LoggerFactory.getLogger(InvokerRepository.class);

    private Class<T> serviceInterface;
    private List<Invoker<T>> invokers = new ArrayList<>();
    private ClientConfig clientConfig;
    private SubscribeInfo subscribeInfo;
    private RegistryPolicy registry;
    private boolean isDirectConn;
    private InvokerFactory invokerFactory;

    private volatile ConcurrentMap<String, Invoker<T>> invokerMap = new ConcurrentHashMap<>(); // <ip:port, Invoker>

    public InvokerRepository(ClientConfig clientCfg) {
        this(clientCfg, false);
    }

    public InvokerRepository(ClientConfig clientCfg, boolean isDirectConn) {
        this.isDirectConn = isDirectConn;
        this.clientConfig = clientCfg;
        this.serviceInterface = (Class<T>) clientCfg.getServiceInterface();
        invokerFactory = obtainInvokerFactory();

        if (isDirectConn) {
            buildDirectConnInvokers();
        }
    }

    @Override
    public synchronized void notify(List<Provider> providers) {
        if (providers == null || providers.isEmpty()) {
            // 不抛出异常，避免订阅时，还没有发现服务导致启动失败
            logger.error("Provider list is empty, {}", subscribeInfo.toString());
        } else {
            logger.info("Update providers, total {} providers", providers.size());
        }
        Set<String> newAddresses = new HashSet<>();
        for (Provider provider : providers) {
            String ip = provider.getIp();
            int port = provider.getPort();
            String address = ip + Constants.COLON + port;
            addOrUpdateInvokers(address, provider);

            newAddresses.add(address);
        }
        List<Invoker> unusedInvokers = findUnusedInvokers(newAddresses);
        refreshInvokers();
        destroyUnusedInvokers(unusedInvokers);
    }

    @Override
    public void added(List<Provider> providers) {
        updated(providers);
    }

    @Override
    public void updated(List<Provider> providers) {
        if (providers == null || providers.isEmpty()) {
            return;
        }
        for (Provider provider : providers) {
            String ip = provider.getIp();
            int port = provider.getPort();
            String address = ip + Constants.COLON + port;
            addOrUpdateInvokers(address, provider);
        }
        refreshInvokers();
    }

    @Override
    public void removed(List<String> ipPorts) {
        List<Invoker> unusedInvokers = new ArrayList<>();
        for (String address : invokerMap.keySet()) {
            if (ipPorts.contains(address)) {
                Invoker unusedInvoker = invokerMap.remove(address);
                unusedInvokers.add(unusedInvoker);
            }
        }
        refreshInvokers();
        destroyUnusedInvokers(unusedInvokers);
    }

    public void destroy() {
        if (!isDirectConn) {
            registry.unsubcribe(subscribeInfo);
            registry.destroy();
        }
        destroyInvokers();
    }

    private List<Invoker> findUnusedInvokers(Set<String> newAddresses) {
        List<Invoker> unusedInvokers = new ArrayList<>();
        for (String address : invokerMap.keySet()) {
            if (!newAddresses.contains(address)) {
                Invoker unusedInvoker = invokerMap.remove(address);
                unusedInvokers.add(unusedInvoker);
            }
        }
        return unusedInvokers;
    }

    private void destroyUnusedInvokers(List<Invoker> unusedInvokers) {
        for (Invoker invoker : unusedInvokers) {
            logger.info("Remove provider {}, do destroy.", invoker.getProvider().toString());
            invoker.destroy();
        }
    }

    private void addOrUpdateInvokers(String address, Provider provider) {
        Invoker invoker = invokerMap.get(address);
        if (invoker == null) {
            // 没有则构建新的invoker
            try {
                invoker = initInvoker(provider);
                invokerMap.put(address, invoker);
                logger.info("Add new provider {}", provider.toString());
            } catch (Exception e) {
                logger.error("Build invoker failed", e);
            }
        } else {
            // 有则更新Provider
           if (invoker.updateProviderIfNeeded(provider)) {
               logger.info("Update provider {}", provider.toString());
           }
        }
    }

    private void buildDirectConnInvokers() {
        List<ClientConfig.RemoteAddress> remoteAddresses = clientConfig.getDirectConnAddress();
        if (remoteAddresses == null || remoteAddresses.isEmpty()) {
            throw new IllegalArgumentException("RemoteAddress config is invalid");
        }
        Exception exeption = null;
        for (ClientConfig.RemoteAddress remoteAddress : remoteAddresses) {
            Provider provider = new Provider(remoteAddress.getIp(), remoteAddress.getPort());
            try {
                Invoker<T> invoker = initInvoker(provider);
                invokers.add(invoker);
            } catch (Exception e) {
                logger.error("Build invoker failed", e);
                exeption = e;
            }
        }
        if (invokers.isEmpty()) {
            throw new RpcException("Build all direct connect invoker failed", exeption);
        }
    }

    private InvokerFactory obtainInvokerFactory() {
        String protocolName = clientConfig.getProtocol();
        if (Constants.ProtocolType.isThrift(protocolName)) {
            // Dorado默认支持Thrift
            protocolName = "dorado";
        }
        try {
            invokerFactory = ExtensionLoader.getExtensionWithName(InvokerFactory.class, protocolName);
        } catch (RpcException e) {
            throw new IllegalArgumentException("Cannot find " + InvokerFactory.class + " with name=" + protocolName + ", please check spi config", e);
        }
        return invokerFactory;
    }

    private Invoker<T> initInvoker(Provider provider) {
        clientConfig.setAddress(provider.getIp(), provider.getPort());
        return invokerFactory.buildInvoker(clientConfig, provider);
    }

    private synchronized void destroyInvokers() {
        if (invokers == null) {
            return;
        }
        for (Invoker invoker : invokers) {
            invoker.destroy();
        }
        invokers.clear();
    }

    private synchronized void refreshInvokers() {
        List<Invoker<T>> discoveryInvokers = new ArrayList<>();
        for (Invoker<T> invoker : invokerMap.values()) {
            discoveryInvokers.add(invoker);
        }
        invokers = discoveryInvokers;
    }

    public synchronized List<Invoker<T>> getInvokers() {
        return invokers;
    }

    public void setSubscribeInfo(SubscribeInfo subscribeInfo) {
        this.subscribeInfo = subscribeInfo;
    }

    public RegistryPolicy getRegistry() {
        return registry;
    }

    public void setRegistry(RegistryPolicy registry) {
        this.registry = registry;
    }

    public Class<T> getServiceInterface() {
        return serviceInterface;
    }

    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    public boolean isDirectConn() {
        return isDirectConn;
    }

}
