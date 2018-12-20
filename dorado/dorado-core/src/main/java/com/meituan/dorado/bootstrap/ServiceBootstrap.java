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
package com.meituan.dorado.bootstrap;

import com.meituan.dorado.bootstrap.invoker.ServiceInvocationRepository;
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.common.extension.ExtensionLoader;
import com.meituan.dorado.common.util.CommonUtil;
import com.meituan.dorado.transport.http.HttpServer;
import com.meituan.dorado.transport.http.HttpServerFactory;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ServiceBootstrap {

    private static HttpServer httpServer;

    public synchronized static void initHttpServer(RpcRole role) {
        // 默认启动http服务，作用于 服务自检
        HttpServerFactory httpServerFactory = ExtensionLoader.getExtension(HttpServerFactory.class);
        if (httpServer == null) {
            httpServer = httpServerFactory.buildServer(role);
        }
    }

    public static HttpServer getHttpServer() {
        if (httpServer != null && httpServer.isStart()) {
            return httpServer;
        } else {
            return null;
        }
    }

    /**
     * 1. 关闭HttpServer
     * 一个进程只需要一个HttpServer，不需要调用该方法，httpServer会在进程退出时销毁
     * 除非服务不需要使用RPC的http端口，可以调用该方法
     * 2. 关闭超时检查任务
     */
    public static void clearGlobalResource() {
        if (httpServer != null) {
            httpServer.close();
            httpServer = null;
        }
        ServiceInvocationRepository.stopTimeoutTask();
    }

    public static Map<String, String> parseRegistryCfg(String registryCfg) {
        if (StringUtils.isBlank(registryCfg)) {
            return Collections.emptyMap();
        }
        Map<String, String> registryInfo = new HashMap<>();
        String[] cfgs = registryCfg.split("://");
        String registryWay = cfgs[0];
        registryInfo.put(Constants.REGISTRY_WAY_KEY, registryWay);
        if (cfgs.length >= 2) {
            String addressParams = cfgs[1];
            String[] paramCfgs = addressParams.split("\\?");
            String registryAddress = paramCfgs[0];
            Map<String, String> attachments = Collections.emptyMap();
            if (paramCfgs.length > 1) {
                attachments = CommonUtil.parseUrlParams(paramCfgs[1]);
            }
            registryInfo.put(Constants.REGISTRY_ADDRESS_KEY, registryAddress);
            registryInfo.putAll(attachments);
        }
        return registryInfo;
    }
}
