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
package com.meituan.dorado.cluster.loadbalance;

import com.meituan.dorado.cluster.LoadBalance;
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.exception.RpcException;
import com.meituan.dorado.common.extension.ExtensionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LoadBalanceFactory {

    private static final Logger logger = LoggerFactory.getLogger(LoadBalanceFactory.class);

    private final static ConcurrentMap<String, LoadBalance> loadBalanceMap = new ConcurrentHashMap<>();

    public static LoadBalance getLoadBalance(String serviceName) {
        LoadBalance loadBalance = loadBalanceMap.get(serviceName);
        if (loadBalance == null) {
            try {
                loadBalance = ExtensionLoader.getExtensionWithName(LoadBalance.class, Constants.DEFAULT_LOADBALANCE_POLICY);
            } catch (RpcException e) {
                throw new RpcException("Not find default " + LoadBalance.class.getName() + ", please check spi config");
            }
            loadBalanceMap.put(serviceName, loadBalance);
        }
        return loadBalance;
    }

    public static void setCustomLoadBalance(String serviceName, LoadBalance loadBalance) {
        loadBalanceMap.put(serviceName, loadBalance);
    }

    public static void setLoadBalance(String serviceName, String loadBalancePolicy) {
        LoadBalance loadBalance;
        try {
            loadBalance = ExtensionLoader.getExtensionWithName(LoadBalance.class, loadBalancePolicy);
        } catch (RpcException e) {
            loadBalance = ExtensionLoader.getExtensionWithName(LoadBalance.class, Constants.DEFAULT_LOADBALANCE_POLICY);
            if (loadBalance != null) {
                logger.warn("Not find loadBalance of loadBalancePolicy:{}, use default loadBalance:{}",
                        loadBalancePolicy, Constants.DEFAULT_LOADBALANCE_POLICY);
            } else {
                throw new RpcException("Not find default " + LoadBalance.class.getName() + ", please check spi config");
            }
        }
        loadBalanceMap.put(serviceName, loadBalance);
    }
}
