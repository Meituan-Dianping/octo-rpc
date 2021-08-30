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
package com.meituan.dorado.serialize.thrift.annotation.codec;

import com.facebook.swift.codec.ThriftCodecManager;
import com.google.common.collect.ImmutableMap;
import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.serialize.thrift.annotation.metadata.ThriftMethodMetadata;
import com.meituan.dorado.serialize.thrift.annotation.metadata.ThriftServiceMetadata;

import java.util.HashMap;
import java.util.Map;

public class ThriftServiceCodec {

    private Map<String, ThriftMethodCodec> methodValueCodecMap;
    private Map<String, ThriftMethodCodec> methodNameCodecMap;

    public ThriftServiceCodec(Object serviceImpl, ThriftCodecManager codecManager, RpcRole rpcRole) {
        this(serviceImpl instanceof Class<?> ? (Class<?>) serviceImpl : serviceImpl.getClass(), codecManager, rpcRole);
    }

    public ThriftServiceCodec(Class<?> clientType, ThriftCodecManager codecManager, RpcRole rpcRole) {
        Map<String, ThriftMethodCodec> name2ProcessorMap = new HashMap<>();
        Map<String, ThriftMethodCodec> value2ProcessorMap = new HashMap<>();

        ThriftServiceMetadata serviceMetadata = new ThriftServiceMetadata(clientType, codecManager.getCatalog());
        for (ThriftMethodMetadata methodMetadata : serviceMetadata.getMethods().values()) {
            // thrift注解可以给method配置alias，所以methodMetadata.getName优先返回alias
            String methodValue = methodMetadata.getName();
            String methodName = methodMetadata.getMethod().getName();
            ThriftMethodCodec methodProcessor = new ThriftMethodCodec(rpcRole, methodMetadata, codecManager);
            if (value2ProcessorMap.containsKey(methodValue)) {
                throw new IllegalArgumentException("Multiple @ThriftMethod-annotated methods named '" + methodValue + "' found in the given services");
            }
            name2ProcessorMap.put(methodName, methodProcessor);
            value2ProcessorMap.put(methodValue, methodProcessor);
        }

        methodValueCodecMap = ImmutableMap.copyOf(value2ProcessorMap);
        methodNameCodecMap = ImmutableMap.copyOf(name2ProcessorMap);
    }

    public ThriftMethodCodec getMethodCodecByValue(String methodValue) {
        return methodValueCodecMap.get(methodValue);
    }

    public ThriftMethodCodec getMethodCodecByName(String methodName) {
        return methodNameCodecMap.get(methodName);
    }
}