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
package com.meituan.dorado.cluster.router;

import com.meituan.dorado.cluster.Router;
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.exception.RpcException;
import com.meituan.dorado.common.extension.ExtensionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RouterFactory {

    private static final Logger logger = LoggerFactory.getLogger(RouterFactory.class);

    private final static ConcurrentMap<String, Router> routerMap = new ConcurrentHashMap<>();

    public static Router getRouter(String serviceName) {
        Router router = routerMap.get(serviceName);
        if (router == null) {
            try {
                router = ExtensionLoader.getExtensionWithName(Router.class, Constants.DEFAULT_ROUTER_POLICY);
            } catch (RpcException e) {
                throw new RpcException("Not find default " + Router.class.getName() + ", please check spi config");
            }
            routerMap.put(serviceName, router);
        }
        return router;
    }


    public static void setRouter(String serviceName, String routerPolicy) {
        Router router;
        try {
            router = ExtensionLoader.getExtensionWithName(Router.class, routerPolicy);
        } catch (RpcException e) {
            router = ExtensionLoader.getExtensionWithName(Router.class, Constants.DEFAULT_ROUTER_POLICY);
            if (router != null) {
                logger.warn("Not find router of routerPolicy:{}, use default loadBalance:{}",
                        routerPolicy, Constants.DEFAULT_ROUTER_POLICY);
            } else {
                throw new RpcException("Not find default Router, please check Router spi config");
            }
        }
        routerMap.put(serviceName, router);
    }
}
