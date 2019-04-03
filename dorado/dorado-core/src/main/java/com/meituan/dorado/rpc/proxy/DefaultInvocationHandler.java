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
package com.meituan.dorado.rpc.proxy;

import com.meituan.dorado.cluster.ClusterHandler;
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.trace.meta.TraceTimeline;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class DefaultInvocationHandler<T> implements InvocationHandler {

    private final ClusterHandler<T> handler;

    public DefaultInvocationHandler(ClusterHandler<T> handler) {
        this.handler = handler;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }
        if ("toString".equals(methodName) && parameterTypes.length == 0) {
            return this.toString();
        }
        if ("hashCode".equals(methodName) && parameterTypes.length == 0) {
            return this.hashCode();
        }
        if ("equals".equals(methodName) && parameterTypes.length == 1) {
            return this.equals(args[0]);
        }

        RpcInvocation invocation = new RpcInvocation(handler.getInterface(), method, args,
                parameterTypes);

        TraceTimeline timeline = TraceTimeline.newRecord(handler.getRepository().getClientConfig().isTimelineTrace(),
                TraceTimeline.INVOKE_START_TS);
        invocation.putAttachment(Constants.TRACE_TIMELINE, timeline);

        return handler.handle(invocation).getReturnVal();
    }
}
