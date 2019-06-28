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

import com.google.common.collect.ImmutableMap;
import com.meituan.dorado.bootstrap.provider.meta.ProviderStatus;
import com.meituan.dorado.bootstrap.provider.meta.ServerInfo;
import com.meituan.dorado.bootstrap.provider.meta.ServiceIfaceInfo;
import com.meituan.dorado.config.service.ProviderConfig;
import com.meituan.dorado.config.service.ServiceConfig;
import com.meituan.dorado.transport.Server;
import com.meituan.dorado.util.MethodUtil;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ProviderInfoRepository {

    private static Date startTime;
    private static String appkey;

    private static final ConcurrentHashMap<Integer, ServerInfo> portServerInfoMap = new ConcurrentHashMap<>();

    private static final ConcurrentMap<Integer, List<ServiceIfaceInfo>> portServicesMap = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Integer> servicePortMap = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Object> serviceImplMap = new ConcurrentHashMap<String, Object>();
    private static final ConcurrentMap<String, Class<?>> serviceIfaceMap = new ConcurrentHashMap<String, Class<?>>();
    private static final ConcurrentMap<String, ImmutableMap<String, Method>> serviceMethodsMap = new ConcurrentHashMap<String, ImmutableMap<String, Method>>();

    protected static void addProviderInfo(ProviderConfig config, Server server) {
        appkey = config.getAppkey();
        if (startTime == null) {
            startTime = new Date();
        }
        int port = config.getPort();
        portServerInfoMap.put(port, new ServerInfo(port, server));

        List<ServiceIfaceInfo> serviceIfaceInfos = new ArrayList<ServiceIfaceInfo>();
        for (ServiceConfig serviceConfig : config.getServiceConfigList()) {
            String serviceName = serviceConfig.getServiceName();
            Class<?> iface = serviceConfig.getServiceInterface();
            Object impl = serviceConfig.getServiceImpl();

            servicePortMap.put(serviceName, port);
            serviceIfaceMap.put(serviceName, iface);
            serviceImplMap.put(serviceName, impl);
            serviceIfaceInfos.add(new ServiceIfaceInfo(serviceName, iface.getName(), impl.getClass().getName()));
        }
        portServicesMap.put(port, serviceIfaceInfos);
    }

    protected static void removeProviderInfo(ProviderConfig config) {
        int port = config.getPort();
        portServerInfoMap.remove(port);

        for (ServiceConfig serviceConfig : config.getServiceConfigList()) {
            String serviceName = serviceConfig.getServiceName();

            servicePortMap.remove(serviceName);
            serviceIfaceMap.remove(serviceName);
            serviceImplMap.remove(serviceName);
        }
        portServicesMap.remove(port);
    }

    public static ConcurrentMap<String, ImmutableMap<String, Method>> getAllMethods() {
        if (serviceMethodsMap.size() != servicePortMap.size()) {
            for (String serviceName : servicePortMap.keySet()) {
                getServiceMethods(serviceName);
            }
        }
        return serviceMethodsMap;
    }

    public static ImmutableMap<String, Method> getServiceMethods(String serviceName) {
        ImmutableMap<String, Method> methodMap = serviceMethodsMap.get(serviceName);
        if (methodMap == null) {
            synchronized (ProviderInfoRepository.class) {
                methodMap = serviceMethodsMap.get(serviceName);
                if (methodMap == null) {
                    Class interfaceClazz = serviceIfaceMap.get(serviceName);
                    ImmutableMap.Builder<String, Method> methodMapBuilder = ImmutableMap.builder();
                    for (Method method : interfaceClazz.getMethods()) {
                        methodMapBuilder.put(MethodUtil.generateMethodSignatureNoIfacePrefix(method), method);
                    }
                    methodMap = methodMapBuilder.build();
                    serviceMethodsMap.putIfAbsent(serviceName, methodMap);
                }
            }
        }
        return methodMap;
    }

    public static ProviderStatus getProviderStatus(int port) {
        ServerInfo serverInfo = portServerInfoMap.get(port);
        if (serverInfo == null || serverInfo.getStatus() == null) {
            return ProviderStatus.DEAD;
        }
        return serverInfo.getStatus();
    }

    public static Date getStartTime() {
        return startTime;
    }

    public static String getAppkey() {
        return appkey;
    }

    public static ConcurrentHashMap<Integer, ServerInfo> getPortServerInfoMap() {
        return portServerInfoMap;
    }

    public static ConcurrentMap<Integer, List<ServiceIfaceInfo>> getPortServicesMap() {
        return portServicesMap;
    }

    public static ConcurrentMap<String, Integer> getServicePortMap() {
        return servicePortMap;
    }

    public static ConcurrentMap<String, Object> getServiceImplMap() {
        return serviceImplMap;
    }

    public static Object getServiceImpl(String serviceName) {
        return serviceImplMap.get(serviceName);
    }

    public static ConcurrentMap<String, Class<?>> getServiceIfaceMap() {
        return serviceIfaceMap;
    }

    public static Class<?> getInterface(String serviceName) {
        return serviceIfaceMap.get(serviceName);
    }

    public static ConcurrentMap<String, ImmutableMap<String, Method>> getServiceMethodsMap() {
        return serviceMethodsMap;
    }


}
