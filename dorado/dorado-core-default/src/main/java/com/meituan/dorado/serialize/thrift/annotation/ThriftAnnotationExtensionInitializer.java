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

import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.config.service.AbstractConfig;
import com.meituan.dorado.bootstrap.ExtensionInitializer;
import com.meituan.dorado.serialize.thrift.ThriftUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThriftAnnotationExtensionInitializer implements ExtensionInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThriftAnnotationExtensionInitializer.class);

    @Override
    public void init(AbstractConfig config, RpcRole rpcRole) {
        Class<?> serviceInterface = config.getServiceInterface();
        if (!ThriftUtil.isAnnotation(serviceInterface)) {
            LOGGER.warn("ServiceInterface={} does not support thrift annotation.", serviceInterface);
            return;
        }
        if (rpcRole == RpcRole.INVOKER) {
            ThriftAnnotationManager.initClient(serviceInterface);
        } else if (rpcRole == RpcRole.PROVIDER) {
            ThriftAnnotationManager.initServer(serviceInterface);
        }
    }
}