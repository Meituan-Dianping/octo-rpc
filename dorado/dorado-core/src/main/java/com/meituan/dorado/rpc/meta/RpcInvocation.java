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
package com.meituan.dorado.rpc.meta;

import com.meituan.dorado.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;

public class RpcInvocation {

    private static final Logger logger = LoggerFactory.getLogger(RpcInvocation.class);
    private static final List<String> FRAMEWORKE_PARAM_KEYS = new ArrayList<String>(Arrays.asList(Constants.RPC_REQUEST,
            Constants.TRACE_PARAM, Constants.TRACE_FILTER_FINISHED, Constants.TRACE_TIMELINE));

    private Class<?> serviceInterface;
    private Method method;
    private Class<?>[] parameterTypes;
    private Object[] arguments;

    private final Map<String, Object> attachments = new HashMap<>();

    public RpcInvocation(Class<?> serviceInterface, Method method, Object[] arguments, Class<?>[] parameterTypes) {
        this.serviceInterface = serviceInterface;
        this.method = method;
        this.arguments = arguments;
        this.parameterTypes = parameterTypes;
    }

    public Class<?> getServiceInterface() {
        return serviceInterface;
    }

    public Method getMethod() {
        return method;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public Object[] getArguments() {
        return arguments;
    }

    public Object getAttachment(String key) {
        return attachments.get(key);
    }

    public void putAttachments(Map<String, Object> attachments) {
        if (attachments == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : attachments.entrySet()) {
            putAttachment(entry.getKey(), entry.getValue());
        }
    }

    public void putAttachment(String key, Object value) {
        if (value == null) {
            return;
        }
        if (containsAttachment(key) && FRAMEWORKE_PARAM_KEYS.contains(key)) {
            logger.warn("Framework param[{}] cannot be put repeatedly.", key);
            return;
        }
        this.attachments.put(key, value);
    }

    public boolean containsAttachment(String key) {
        return this.attachments.containsKey(key);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RpcInvocation{")
                .append("serviceInterface=").append(serviceInterface)
                .append(", method=").append(method)
                .append(", arguments").append(arguments)
                .append(", parameterTypes").append(parameterTypes)
                .append(", attachments").append(attachments)
                .append("}");
        return sb.toString();
    }
}
