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
import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.common.util.JacksonUtils;
import com.meituan.dorado.rpc.GenericService;
import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.rpc.meta.RpcResult;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GenericInvokerFilter implements Filter {

    private static final List<String> genericMethodNameList = Arrays.asList(new String[]{"$invoke"});

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }

    @Override
    public RpcResult filter(RpcInvocation invocation, FilterHandler handler) throws Throwable {
        String genericType = (String) invocation.getAttachment(Constants.GENERIC_KEY);
        // 配置泛化标识 & 方法为泛化方法
        if (genericType != null && genericMethodNameList.contains(invocation.getMethod().getName())) {
            invocation = doSerializeParameters(invocation, genericType);
        }
        RpcResult result = handler.handle(invocation);
        return result;
    }

    @Override
    public RpcRole getRole() {
        return RpcRole.INVOKER;
    }

    private RpcInvocation doSerializeParameters(RpcInvocation invocation, String genericType) throws Exception {
        Object[] arguments = invocation.getArguments();
        if (arguments == null || arguments.length < 3) {
            throw new IllegalArgumentException("Request arguments is illegal.");
        }

        // 参数值为List<String>不需要序列化
        if (arguments[2] != null && (arguments[2] instanceof List)) {
            return invocation;
        }

        Object[] paramValues = (Object[]) arguments[2];
        List<String> decorateParamValues = new ArrayList<>();
        if (Constants.GenericType.JACKSON_JSON_DEFAULT.getValue().equals(genericType)
                || Constants.GenericType.JACKSON_JSON_COMMON.getValue().equals(genericType)) {
            for (int i = 0; i < paramValues.length; i++) {
                decorateParamValues.add(JacksonUtils.serializeUnchecked(paramValues[i]));
            }
        } else if (Constants.GenericType.JACKSON_JSON_SIMPLE.getValue().equals(genericType)) {
            for (int i = 0; i < paramValues.length; i++) {
                decorateParamValues.add(JacksonUtils.simpleSerializeUnchecked(paramValues[i]));
            }
        }
        arguments[2] = decorateParamValues;

        Method method = GenericService.class.getMethod("$invoke", String.class, List.class, List.class);
        Class<?>[] clazz = method.getParameterTypes();

        RpcInvocation rpcInvocation = new RpcInvocation(invocation.getServiceInterface(), method, arguments, clazz);
        rpcInvocation.putAttachments(invocation.getAttachments());
        return rpcInvocation;
    }
}