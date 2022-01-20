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

import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.common.exception.RpcException;
import com.meituan.dorado.common.util.CommonUtil;
import com.meituan.dorado.rpc.handler.filter.AbstractTraceFilter;
import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.trace.InvokeTrace;
import com.meituan.dorado.trace.TraceFactory;
import com.meituan.dorado.trace.meta.TraceParam;
import com.meituan.dorado.trace.meta.TraceTimeline;
import com.meituan.dorado.transport.meta.Request;

import java.util.List;

public abstract class AbstractProviderTraceFilter extends AbstractTraceFilter {

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE - 1;
    }

    @Override
    public RpcRole getRole() {
        return RpcRole.PROVIDER;
    }

    @Override
    public void preHandle(RpcInvocation invocation) {
        TraceTimeline.record(TraceTimeline.FILTER_START_TS, invocation);

        Request request = (Request) invocation.getAttachment(Constants.RPC_REQUEST);
        if (request == null) {
            throw new RpcException("No request info in RpcInvocation");
        }

        TraceTimeline timeline = CommonUtil.objectToClazzObj(invocation.getAttachment(Constants.TRACE_TIMELINE), TraceTimeline.class);
        TraceParam traceParam = genTraceParam(request, timeline);
        invocation.putAttachment(Constants.TRACE_PARAM, traceParam);

        List<InvokeTrace> traceList = TraceFactory.getInvokeTrace();
        for (InvokeTrace trace : traceList) {
            trace.serverRecv(traceParam, invocation);
        }
    }

    @Override
    public void postHandle(RpcInvocation invocation, Throwable ex) {
        Request request = (Request) invocation.getAttachment(Constants.RPC_REQUEST);
        TraceParam traceParam = (TraceParam) invocation.getAttachment(Constants.TRACE_PARAM);
        if (request == null || traceParam == null) {
            throw new RpcException("No request or traceParam info in RpcInvocation");
        }
        traceParam.setRequestSize(CommonUtil.objectToInt(request.getAttachment(Constants.REQUEST_SIZE), -1));
        // 此处还未序列化返回包大小未知
        traceParam.setResponseSize(-1);
        traceParam.setThrowable(ex);

        TraceTimeline.record(TraceTimeline.FILTER_END_TS, invocation);

        List<InvokeTrace> traceList = TraceFactory.getInvokeTrace();
        for (InvokeTrace trace : traceList) {
            trace.serverSend(traceParam, invocation);
        }

        // trace report状态记录
        invocation.putAttachment(Constants.TRACE_FILTER_FINISHED, Constants.TRUE);
        if (!TraceTimeline.isEnable(invocation)) {
            invocation.putAttachment(Constants.TRACE_REPORT_FINISHED, Constants.TRUE);
        }
    }

    protected abstract TraceParam genTraceParam(Request request, TraceTimeline timeline);
}
