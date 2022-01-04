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
package com.meituan.dorado.check.http;

import com.google.common.collect.ImmutableMap;
import com.meituan.dorado.bootstrap.ServiceBootstrap;
import com.meituan.dorado.bootstrap.provider.ProviderInfoRepository;
import com.meituan.dorado.bootstrap.provider.meta.ProviderStatus;
import com.meituan.dorado.bootstrap.provider.meta.ServerInfo;
import com.meituan.dorado.bootstrap.provider.meta.ServiceIfaceInfo;
import com.meituan.dorado.check.http.meta.HttpURI;
import com.meituan.dorado.check.http.meta.PortServiceInfo;
import com.meituan.dorado.check.http.meta.ProviderInfo;
import com.meituan.dorado.check.http.meta.ServiceMethodInfo;
import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.common.extension.ExtensionLoader;
import com.meituan.dorado.common.util.URLUtil;
import com.meituan.dorado.rpc.handler.http.HttpInvokeHandler;
import com.meituan.dorado.transport.http.HttpSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

import static com.meituan.dorado.check.http.meta.HttpURI.SERVICE_INVOKE_PREFIX;
import static com.meituan.dorado.check.http.meta.HttpURI.toHttpCheckURI;

public class DoradoHttpCheckHandler implements HttpCheckHandler {

    private static final Logger logger = LoggerFactory.getLogger(DoradoHttpCheckHandler.class);

    private HttpInvokeHandler httpInvokeHandler;
    private RpcRole role;
    private Set<String> supportReqs;


    public DoradoHttpCheckHandler() {
        try {
            httpInvokeHandler = ExtensionLoader.getExtension(HttpInvokeHandler.class);
        } catch (Exception e) {
            logger.warn("Get instance of {} failed.", HttpInvokeHandler.class.getSimpleName(), e);
        }
    }

    @Override
    public void handle(HttpSender httpSender, String uri, byte[] content, Map<String, String> headers) {
        String path = URLUtil.getURIPath(uri);

        /**
         * http接口调用
         */
        if (path.startsWith(SERVICE_INVOKE_PREFIX.uri())) {
            if (RpcRole.PROVIDER == role) {
                String errorMsg = role + " not support service invoke";
                logger.warn(errorMsg);
                httpSender.sendErrorResponse(errorMsg);
                return;
            }
            if (httpInvokeHandler == null) {
                String errorMsg = "No instance of " + HttpInvokeHandler.class.getName();
                logger.warn(errorMsg);
                httpSender.sendErrorResponse(errorMsg);
            }
            httpInvokeHandler.handle(httpSender, uri, content, headers);
            return;
        }

        if (!getSupportReqs().contains(path)) {
            httpSender.sendErrorResponse(role + " not support the request. Support uri: " + supportReqs);
            return;
        }
        try {
            handleHttpCheckReq(httpSender, uri, content);
        } catch (Exception e) {
            logger.warn("Http check failed.", e);
            httpSender.sendErrorResponse(e.getClass().getName() + ":" + e.getMessage());
        }
    }

    @Override
    public void setRole(RpcRole rpcRole) {
        if (role != null && role != rpcRole) {
            role = RpcRole.MULTIROLE;
            updateSupportReqs();
        } else {
            role = rpcRole;
        }
    }

    @Override
    public RpcRole getRole() {
        return role;
    }

    private void handleHttpCheckReq(HttpSender httpSender, String uri, byte[] content) {
        Map<String, String> kvParams = new HashMap<String, String>();
        String path = URLUtil.getURIPathAndParameter(uri, kvParams);
        switch (toHttpCheckURI(path)) {
            case SERVICE_BASE_INFO:
                getServiceBaseInfo(httpSender);
                break;
            case SERVICE_METHOD_INFO:
                getServiceMethodInfo(httpSender);
                break;
            default:
                httpSender.sendErrorResponse("not support the request");
        }
    }

    /**
     * 服务基本信息
     * 角色: 服务端
     *
     * @param httpSender
     */
    /**
     * 服务基本信息
     * 角色: 服务端
     *
     * @param httpSender
     */
    private void getServiceBaseInfo(HttpSender httpSender) {
        ProviderInfo info = new ProviderInfo();
        info.setAppkey(ProviderInfoRepository.getAppkey());
        info.setStartTime(ProviderInfoRepository.getStartTime().toString());

        List<PortServiceInfo> serviceInfos = new ArrayList<>();
        for (Map.Entry<Integer, ServerInfo> entry : ProviderInfoRepository.getPortServerInfoMap().entrySet()) {
            int port = entry.getKey();
            String status = ProviderInfoRepository.getProviderStatus(port).name();

            List<ServiceIfaceInfo> serviceIfaceInfos = ProviderInfoRepository.getPortServicesMap().get(port);
            PortServiceInfo portServiceInfo = new PortServiceInfo(String.valueOf(port), serviceIfaceInfos, status);
            serviceInfos.add(portServiceInfo);
        }

        int httpPort = ServiceBootstrap.getHttpServer().getLocalAddress().getPort();
        serviceInfos.add(new PortServiceInfo(httpPort + "(http)", Collections.EMPTY_LIST, ProviderStatus.ALIVE.name()));
        info.setServiceInfo(serviceInfos);

        httpSender.sendObjectJson(info);
    }

    /**
     * 服务方法信息
     * 角色: 服务端
     *
     * @param httpSender
     */
    private void getServiceMethodInfo(HttpSender httpSender) {
        ProviderInfo info = new ProviderInfo();
        info.setAppkey(ProviderInfoRepository.getAppkey());

        List<ServiceMethodInfo> serviceMethods = new ArrayList<ServiceMethodInfo>();
        ConcurrentMap<String, ImmutableMap<String, Method>> serviceMethodsMap = ProviderInfoRepository.getAllMethods();
        for (Map.Entry<String, ImmutableMap<String, Method>> entry : serviceMethodsMap.entrySet()) {
            String serviceName = entry.getKey();
            ImmutableMap<String, Method> methods = entry.getValue();
            Set<String> methodNames = new HashSet<String>();
            for (String methodName : methods.keySet()) {
                String returnType = methods.get(methodName).getReturnType().getSimpleName();
                methodNames.add(methodName + ":" + returnType);
            }
            ServiceMethodInfo methodsInfo = new ServiceMethodInfo(serviceName, methodNames);
            serviceMethods.add(methodsInfo);
        }
        info.setServiceMethods(serviceMethods);

        httpSender.sendObjectJson(info);
    }

    private synchronized Set<String> getSupportReqs() {
        if (supportReqs == null) {
            supportReqs = HttpURI.getSupportUriOfRole(role);
        }
        return supportReqs;
    }

    private synchronized void updateSupportReqs() {
        supportReqs = HttpURI.getSupportUriOfRole(role);
    }
}
