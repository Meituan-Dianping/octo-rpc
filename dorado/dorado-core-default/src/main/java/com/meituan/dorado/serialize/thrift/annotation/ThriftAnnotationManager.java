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
package com.meituan.dorado.serialize.thrift.annotation;

import com.facebook.swift.codec.ThriftCodecManager;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.meituan.dorado.bootstrap.provider.ProviderInfoRepository;
import com.meituan.dorado.serialize.thrift.annotation.codec.ThriftServiceCodec;

public class ThriftAnnotationManager {

    private static final ThriftCodecManager codecManager = new ThriftCodecManager();

    private static LoadingCache<Class<?>, ThriftServiceCodec> clientCodecCache;
    private static LoadingCache<Class<?>, ThriftServiceCodec> serverCodecCache;

    public static void initClient(Class<?> serviceInterface) {
        if (clientCodecCache == null) {
            synchronized (ThriftAnnotationExtensionInitializer.class) {
                if (clientCodecCache == null) {
                    clientCodecCache = CacheBuilder
                            .newBuilder()
                            .build(new CacheLoader<Class<?>, ThriftServiceCodec>() {

                                @Override
                                public ThriftServiceCodec load(Class<?> serviceInterface) {
                                    return new ThriftServiceCodec(serviceInterface, codecManager);
                                }
                            });
                }
            }
        }
        clientCodecCache.getUnchecked(serviceInterface);
    }

    public static void initServer(Class<?> serviceInterface) {
        if (serverCodecCache == null) {
            synchronized (ThriftAnnotationExtensionInitializer.class) {
                if (serverCodecCache == null) {
                    serverCodecCache = CacheBuilder
                            .newBuilder()
                            .build(new CacheLoader<Class<?>, ThriftServiceCodec>() {

                                @Override
                                public ThriftServiceCodec load(Class<?> serviceInterface) {
                                    Object serviceImpl = ProviderInfoRepository.getServiceImpl(serviceInterface.getName());
                                    return new ThriftServiceCodec(serviceImpl, codecManager);
                                }
                            });
                }
            }
        }
        serverCodecCache.getUnchecked(serviceInterface);
    }

    public static ThriftServiceCodec getClientCodec(Class<?> serviceInterface) {
        return clientCodecCache.getUnchecked(serviceInterface);
    }

    public static ThriftServiceCodec getServerCodec(Class<?> serviceInterface) {
        return serverCodecCache.getUnchecked(serviceInterface);
    }
}