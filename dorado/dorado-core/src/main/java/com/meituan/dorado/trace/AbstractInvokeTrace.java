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
package com.meituan.dorado.trace;

import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.util.CommonUtil;
import com.meituan.dorado.rpc.AsyncContext;
import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.trace.meta.TraceParam;
import com.meituan.dorado.trace.meta.TraceTimeline;
import com.meituan.dorado.transport.meta.Request;
import com.meituan.dorado.transport.meta.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class AbstractInvokeTrace implements InvokeTrace {

    private static final Logger logger = LoggerFactory.getLogger(AbstractInvokeTrace.class);

    @Override
    public void clientRecv(TraceParam traceParam, RpcInvocation invocation) {
        if (AsyncContext.isAsyncReq(invocation)) {
            asyncClientRecv(traceParam, invocation);
        } else {
            syncClientRecv(traceParam, invocation);
        }
    }

    @Override
    public void serverSend(TraceParam traceParam, RpcInvocation invocation) {
        if (TraceTimeline.isEnable(invocation)) {
            if (Boolean.parseBoolean((String) invocation.getAttachment(Constants.TRACE_FILTER_FINISHED))) {
                serverSendInCodec(traceParam, invocation);
            }
        } else {
            serverSendInFilter(traceParam, invocation);
        }
    }

    protected abstract void asyncClientRecv(TraceParam traceParam, RpcInvocation invocation);

    protected abstract void syncClientRecv(TraceParam traceParam, RpcInvocation invocation);

    protected abstract void serverSendInCodec(TraceParam traceParam, RpcInvocation invocation);

    protected abstract void serverSendInFilter(TraceParam traceParam, RpcInvocation invocation);

    public static void reportServerTraceInfoIfNeeded(Request request, Response response) {
        if (TraceFactory.getInvokeTrace().isEmpty()) {
            return;
        }
        RpcInvocation invocation = request.getData();
        if (!Boolean.parseBoolean((String) invocation.getAttachment(Constants.TRACE_REPORT_FINISHED))) {
            TraceParam traceParam = (TraceParam) invocation.getAttachment(Constants.TRACE_PARAM);
            if (traceParam == null) {
                logger.warn("No traceParam info in RpcInvocation, cannot do trace report.");
                return;
            }
            if (response != null) {
                traceParam.setResponseSize(CommonUtil.objectToInt(response.getAttachment(Constants.RESPONSE_SIZE), -1));
            }

            List<InvokeTrace> traceList = TraceFactory.getInvokeTrace();
            for (InvokeTrace trace : traceList) {
                trace.serverSend(traceParam, invocation);
            }
            invocation.putAttachment(Constants.TRACE_REPORT_FINISHED, Constants.TRUE);
        }
    }
}
