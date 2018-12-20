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
package com.meituan.dorado.rpc.handler.provider;

import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.common.exception.ApplicationException;
import com.meituan.dorado.rpc.handler.InvokeHandler;
import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.rpc.meta.RpcResult;
import com.meituan.dorado.trace.meta.TraceTimeline;
import com.meituan.dorado.transport.meta.Request;
import com.meituan.dorado.transport.meta.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractProviderInvokeHandler implements InvokeHandler {

    private static final Logger logger = LoggerFactory.getLogger(AbstractProviderInvokeHandler.class);

    @Override
    public Response handle(Request request) {
        Response response = buildResponse(request);

        RpcInvocation invocation = request.getData();
        TraceTimeline.record(TraceTimeline.BIZ_CALL_START_TS, invocation);

        RpcResult rpcResult = new RpcResult();

        try {
            Object result = invocation.getMethod().invoke(request.getServiceImpl(), invocation.getArguments());
            rpcResult.setReturnVal(result);
        } catch (Exception excp) {
            rpcResult.setReturnVal(excp);
            if (excp instanceof ApplicationException) {
                response.setException(excp);
            } else {
                response.setException(new ApplicationException(excp.getCause() != null ? excp.getCause() : excp));
            }
        }
        TraceTimeline.record(TraceTimeline.BIZ_CALL_END_TS, invocation);

        response.setResult(rpcResult);
        return response;
    }

    @Override
    public RpcRole getRole() {
        return RpcRole.PROVIDER;
    }
}
