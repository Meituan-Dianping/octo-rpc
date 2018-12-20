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
import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.rpc.meta.RpcResult;
import com.meituan.dorado.trace.TraceFactory;

public abstract class AbstractTraceFilter implements Filter {

    public String getName() {
        return this.getClass().getName();
    }

    protected abstract void preHandle(RpcInvocation invocation);

    protected abstract void postHandle(RpcInvocation invocation, Throwable e);

    @Override
    public RpcResult filter(RpcInvocation invocation, FilterHandler nextHandler) throws Throwable {
        if (TraceFactory.getInvokeTrace().isEmpty()) {
            // 没有监控模块，则状态置为ok
            invocation.putAttachment(Constants.TRACE_REPORT_FINISHED, Constants.TRUE);
            return nextHandler.handle(invocation);
        }

        RpcResult result;
        Throwable exception = null;
        try {
            preHandle(invocation);
            result = nextHandler.handle(invocation);
        } catch (Throwable e) {
            exception = e;
            throw e;
        } finally {
            postHandle(invocation, exception);
        }
        return result;
    }
}
