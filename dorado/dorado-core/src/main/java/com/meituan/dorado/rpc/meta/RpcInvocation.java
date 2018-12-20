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
import java.util.HashMap;
import java.util.Map;

public class RpcInvocation {

    private static final Logger logger = LoggerFactory.getLogger(RpcInvocation.class);

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
        this.attachments.putAll(attachments);
    }

    public void putAttachment(String key, Object value) {
        if (value == null) {
            return;
        }
        if (containsAttachment(key) && (Constants.RPC_REQUEST.equals(key) || Constants.TRACE_PARAM.equals(key) ||
                Constants.TRACE_FILTER_FINISHED.equals(key) || Constants.TRACE_TIMELINE.equals(key))) {
            logger.warn("Framework param[{}] cannot be put repeatedly.", key);
            return;
        }
        this.attachments.put(key, value);
    }

    public boolean containsAttachment(String key) {
        return this.attachments.containsKey(key);
    }

    public Object removeAttachment(String key) {
        if (Constants.RPC_REQUEST.equals(key) || Constants.TRACE_PARAM.equals(key) ||
                Constants.TRACE_FILTER_FINISHED.equals(key) || Constants.TRACE_TIMELINE.equals(key)) {
            logger.warn("Framework param[{}] cannot be removed.", key);
            return this;
        }
        return this.attachments.remove(key);
    }
}
