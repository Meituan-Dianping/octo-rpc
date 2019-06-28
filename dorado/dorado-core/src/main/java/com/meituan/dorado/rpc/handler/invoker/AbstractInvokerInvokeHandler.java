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
package com.meituan.dorado.rpc.handler.invoker;

import com.meituan.dorado.bootstrap.invoker.ServiceInvocationRepository;
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.common.exception.RemoteException;
import com.meituan.dorado.common.exception.RpcException;
import com.meituan.dorado.common.exception.TimeoutException;
import com.meituan.dorado.rpc.AsyncContext;
import com.meituan.dorado.rpc.DefaultFuture;
import com.meituan.dorado.rpc.ResponseFuture;
import com.meituan.dorado.rpc.handler.InvokeHandler;
import com.meituan.dorado.rpc.meta.RpcResult;
import com.meituan.dorado.trace.meta.TraceTimeline;
import com.meituan.dorado.transport.meta.Request;
import com.meituan.dorado.transport.meta.Response;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public abstract class AbstractInvokerInvokeHandler implements InvokeHandler {

    @Override
    public Response handle(Request request) throws Throwable {
        DefaultFuture future = new DefaultFuture(request);
        request.putAttachment(Constants.RESPONSE_FUTURE, future);
        ServiceInvocationRepository.putRequestAndFuture(request, future);

        TraceTimeline.record(TraceTimeline.FILTER_FIRST_STAGE_END_TS, request.getData());
        try {
            request.getClient().request(request);
            if (AsyncContext.isAsyncReq(request.getData())) {
                return handleAsync(request, future);
            } else {
                return handleSync(request, future);
            }
        } finally {
            TraceTimeline.record(TraceTimeline.FILTER_SECOND_STAGE_START_TS, request.getData());
        }
    }

    private Response handleAsync(Request request, ResponseFuture future) {
        ServiceInvocationRepository.addTimeoutTask(request, future);
        AsyncContext.getContext().setFuture(future);
        Response response = buildResponse(request);
        response.setResult(new RpcResult());
        return response;
    }

    private Response handleSync(Request request, ResponseFuture future) {
        try {
            future.get(future.getTimeout(), TimeUnit.MILLISECONDS);
            return future.getResponse();
        } catch (TimeoutException e) {
            throw e;
        } catch (ExecutionException e) {
            if (e.getCause() != null) {
                Throwable exception = e.getCause();
                if (exception instanceof CancellationException || exception instanceof InterruptedException) {
                    throw new RpcException(exception);
                }
                if (exception instanceof RemoteException) {
                    throw (RemoteException) exception;
                }
                String remotehostPort = getRemoteHostPort(request);
                throw new RemoteException("Remote invoke failed, interface=" + request.getServiceName() + "|method=" + request.getData().getMethod().getName() + "|provider=" +
                        remotehostPort, exception);
            }
            String remotehostPort = getRemoteHostPort(request);
            throw new RemoteException("Remote invoke failed, interface=" + request.getServiceName() + "|method=" + request.getData().getMethod().getName() + "|provider=" +
                    remotehostPort, e);
        } catch (Throwable e) {
            String remoteHostPort = getRemoteHostPort(request);
            throw new RemoteException("Remote invoke failed, interface=" + request.getServiceName() + "|method=" + request.getData().getMethod().getName() + "|provider=" +
                    remoteHostPort, e);
        } finally {
            ServiceInvocationRepository.removeAndGetFuture(request.getSeq());
        }
    }

    protected String getRemoteHostPort(Request request) {
        String hostPort = "unKnown";
        if (request.getClient() != null) {
            String host = request.getClient().getRemoteAddress().getHostName();
            int port = request.getClient().getRemoteAddress().getPort();
            hostPort = host + Constants.COLON + port;
        }
        return hostPort;
    }

    @Override
    public RpcRole getRole() {
        return RpcRole.INVOKER;
    }
}
