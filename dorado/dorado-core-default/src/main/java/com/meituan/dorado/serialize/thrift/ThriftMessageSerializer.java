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
package com.meituan.dorado.serialize.thrift;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class ThriftMessageSerializer {

    protected static final ConcurrentMap<String, Method> cachedMethod = new ConcurrentHashMap<>();
    protected static final ConcurrentMap<String, Class<?>> cachedClass = new ConcurrentHashMap<>();

    protected abstract byte[] serialize(Object obj) throws Exception;

    protected abstract Object deserialize4OctoThrift(byte[] buff, Object obj) throws Exception;

    protected abstract Object deserialize4Thrift(byte[] buff, Class<?> iface, Map<String, Object> attachments) throws Exception;

    public static class ThriftMessageInfo {
        String methodName;
        int seqId;

        public ThriftMessageInfo(String methodName, int seqId) {
            this.methodName = methodName;
            this.seqId = seqId;
        }
    }
}
