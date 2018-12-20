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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meituan.dorado.bootstrap.ServiceBootstrap;
import com.meituan.dorado.bootstrap.provider.ProviderStatus;
import com.meituan.dorado.bootstrap.provider.ServerInfo;
import com.meituan.dorado.bootstrap.provider.ServicePublisher;
import com.meituan.dorado.check.http.meta.ServiceIfaceInfo;
import com.meituan.dorado.check.http.meta.ServiceInfo;
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.common.extension.ExtensionLoader;
import com.meituan.dorado.common.util.URLUtil;
import com.meituan.dorado.rpc.handler.http.DefaultHttpResponse;
import com.meituan.dorado.rpc.handler.http.HttpInvokeHandler;
import com.meituan.dorado.transport.http.HttpSender;
import com.meituan.dorado.transport.http.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DoradoHttpCheckHandler implements HttpCheckHandler {

    private static final Logger logger = LoggerFactory.getLogger(DoradoHttpCheckHandler.class);

    public static final String SERVICE_REQUEST_PREFIX = "/invoke/";

    private static final String SERVICE_BASE_INFO = "/service.info";
    private static final String CALL_INFO = "/call.info";
    private static final String AUTH_INFO = "/auth.info";
    private Set<String> supportReqs;

    private HttpInvokeHandler httpInvokeHandler;
    private RpcRole role;

    public DoradoHttpCheckHandler() {
        httpInvokeHandler = ExtensionLoader.getExtension(HttpInvokeHandler.class);
    }

    @Override
    public void handle(HttpSender httpSender, String uri, byte[] content) {
        String path = URLUtil.getURIPath(uri);

        if (path.startsWith(SERVICE_REQUEST_PREFIX)) {
            if (RpcRole.INVOKER == role) {
                String errorMsg = role + " not support service invoke";
                logger.warn(errorMsg);
                httpSender.sendErrorResponse(errorMsg);
            } else if (httpInvokeHandler == null) {
                String errorMsg = "no http invoke service";
                logger.warn(errorMsg);
                httpSender.sendErrorResponse(errorMsg);
            } else {
                httpInvokeHandler.handle(httpSender, uri, content);
            }
            return;
        }

        if (!getSupportReqs().contains(path)) {
            httpSender.sendErrorResponse(role + " not support the request");
            return;
        }

        handleHttpCheckReq(httpSender, uri, content);
    }

    private void handleHttpCheckReq(HttpSender httpSender, String uri, byte[] content) {
        String path = URLUtil.getURIPath(uri);

        if (SERVICE_BASE_INFO.equals(path)) {
            getServiceBaseInfo(httpSender);
        } else if (CALL_INFO.equals(path)) {
            getCallInfo(httpSender);
        } else if (AUTH_INFO.equals(path)) {
            getAuthInfo(httpSender);
        } else {
            httpSender.sendErrorResponse(" not support the request");
        }
    }

    /**
     * 服务Appkey，HttpServer端口号，NettyServer端口号，服务接口列表，版本号
     */
    private void getServiceBaseInfo(HttpSender httpSender) {
        ServiceInfo info = new ServiceInfo();
        info.setAppkey(ServicePublisher.getAppkey());

        Map<String, String> portStatus = new HashMap<>();
        for (Map.Entry<Integer, ProviderStatus> entry : ServerInfo.getServerStatus().entrySet()) {
            portStatus.put(entry.getKey().toString(), entry.getValue().name());
        }
        HttpServer httpServer = ServiceBootstrap.getHttpServer();
        if (httpServer != null) {
            int httpPort = ServiceBootstrap.getHttpServer().getLocalAddress().getPort();
            portStatus.put(httpPort + "(http)", ProviderStatus.ALIVE.name());
            info.setPortStatus(portStatus);
        }

        Map<String, ServiceIfaceInfo> serviceIfaceInfos = new HashMap<>();
        for (Map.Entry<String, Class<?>> entry : ServicePublisher.getServiceInterfaceMap().entrySet()) {
            String serviceName = entry.getKey();

            String ifaceName = entry.getValue().getName();
            String implName = ServicePublisher.getServiceImplMap().get(serviceName).getClass().getName();
            int port = ServicePublisher.getServiceServerMap().get(serviceName).getLocalAddress().getPort();
            String portStr = String.valueOf(port);
            ServiceIfaceInfo serviceIfaceInfo = new ServiceIfaceInfo(portStr, ifaceName, implName);
            serviceIfaceInfos.put(serviceName, serviceIfaceInfo);
        }
        info.setServiceInfo(serviceIfaceInfos);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(info);
            DefaultHttpResponse response = new DefaultHttpResponse(bytes, Constants.CONTENT_TYPE_JSON);
            httpSender.send(response);
        } catch (JsonProcessingException e) {
            logger.error("Object to json fail", e);
            httpSender.sendErrorResponse("Server handle fail: return object to json fail");
        }
    }

    private void getAuthInfo(HttpSender httpSender) {
        DefaultHttpResponse response = new DefaultHttpResponse("Http自检支持中。。。。。。".getBytes(), Constants.CONTENT_TYPE_JSON);
        httpSender.send(response);
    }

    /**
     * Invoker: 查询所调用服务的信息
     * <p>
     * Provicer:
     */
    private void getCallInfo(HttpSender httpSender) {
        DefaultHttpResponse response = new DefaultHttpResponse("Http自检支持中。。。。。。".getBytes(), Constants.CONTENT_TYPE_JSON);
        httpSender.send(response);
    }

    private Set<String> getSupportReqs() {
        if (supportReqs == null) {
            synchronized (this) {
                if (supportReqs == null) {
                    supportReqs = getSupportReqsByRole();
                }
            }
        }
        return supportReqs;
    }

    private Set<String> getSupportReqsByRole() {
        Set<String> roleReqs = new HashSet<>();
        if (RpcRole.MULTIROLE == role || RpcRole.PROVIDER == role) {
            roleReqs.add(SERVICE_BASE_INFO);
            roleReqs.add(CALL_INFO);
            roleReqs.add(AUTH_INFO);
        } else {
            roleReqs.add(CALL_INFO);
            roleReqs.add(AUTH_INFO);
        }
        return roleReqs;
    }

    @Override
    public void setRole(RpcRole rpcRole) {
        if (role != null && role != rpcRole) {
            role = RpcRole.MULTIROLE;
        } else {
            role = rpcRole;
        }
        if (RpcRole.PROVIDER == rpcRole && httpInvokeHandler == null) {
            logger.warn("No {} implement, please check spi config", HttpInvokeHandler.class.getName());
        }
    }

    @Override
    public RpcRole getRole() {
        return role;
    }
}
