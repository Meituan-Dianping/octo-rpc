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
package com.meituan.dorado.rpc.handler.filter;

import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.exception.RpcException;
import com.meituan.dorado.rpc.handler.provider.AbstractProviderFilter;
import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.rpc.meta.RpcResult;
import com.meituan.dorado.transport.meta.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessLogFilter extends AbstractProviderFilter {

    private static final Logger logger = LoggerFactory.getLogger(AccessLogFilter.class);

    @Override
    public RpcResult filter(RpcInvocation invocation, FilterHandler handler) throws Throwable {
        Request request = (Request)invocation.getAttachment(Constants.RPC_REQUEST);
        if (request == null) {
            throw new RpcException("No request info in RpcInvocation");
        }
        logger.debug("Request({}):{}", request.getSeq(),
                request.getServiceName() + "#" + request.getData().getMethod().getName());
        RpcResult result = handler.handle(invocation);
        logger.debug("Response({}):{}", request.getSeq(), result.toString());
        return result;
    }
}
