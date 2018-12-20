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
package com.meituan.dorado.rpc.handler.http;

import com.meituan.dorado.bootstrap.provider.ServicePublisher;
import com.meituan.dorado.check.http.DoradoHttpCheckHandler;
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.common.util.URLUtil;
import com.meituan.dorado.transport.http.HttpSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * URI 是invoke开头的则认为是接口请求，否则认为是http check
 * 接口请求：/invoke/com.meituan.dorado.test.HelloService
 */
public class DoradoHttpInvokeHandler implements HttpInvokeHandler {

    private static final Logger logger = LoggerFactory.getLogger(DoradoHttpInvokeHandler.class);

    @Override
    public void handle(HttpSender httpSender, String uri, byte[] content) {

        String path = URLUtil.getURIPath(uri);
        if (!path.startsWith(DoradoHttpCheckHandler.SERVICE_REQUEST_PREFIX)) {
            String errorMsg = "Invalid service invoke, must have prefix:" + DoradoHttpCheckHandler.SERVICE_REQUEST_PREFIX;
            logger.warn(errorMsg);
            httpSender.sendErrorResponse(errorMsg);
            return;
        }
        String serviceName = getServiceNameByPath(path);
        Class<?> interfaceName = ServicePublisher.getInterface(serviceName);
        if (interfaceName == null) {
            String errorMsg = "No service interface of serviceName:" + serviceName;
            logger.warn(errorMsg);
            httpSender.sendErrorResponse(errorMsg);
            return;
        }

        DefaultHttpResponse response = new DefaultHttpResponse("接口请求支持中".getBytes(), Constants.CONTENT_TYPE_JSON);
        httpSender.send(response);
    }

    @Override
    public void setRole(RpcRole role) {
        // do nothing, just Provider support invoked
    }

    @Override
    public RpcRole getRole() {
        return RpcRole.PROVIDER;
    }

    private String getServiceNameByPath(String path) {
        String serviceName = path.substring(DoradoHttpCheckHandler.SERVICE_REQUEST_PREFIX.length());
        return serviceName;
    }
}
