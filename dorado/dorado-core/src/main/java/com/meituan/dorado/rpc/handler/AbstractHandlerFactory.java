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
package com.meituan.dorado.rpc.handler;

import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.common.extension.ExtensionLoader;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractHandlerFactory implements HandlerFactory {

    private static ConcurrentMap<RpcRole, InvokeHandler> serviceInvokeHandlers = new ConcurrentHashMap<>();
    private static InvokeHandler heartBeatInvokeHandler = null;

    @Override
    public InvokeHandler getInvocationHandler(int messageType, RpcRole rpcRole) {
        if (Constants.MESSAGE_TYPE_HEART == messageType) {
            if (heartBeatInvokeHandler == null) {
                heartBeatInvokeHandler = createHeartBeatInvocationHandler();
            }
            return heartBeatInvokeHandler;
        } else if (Constants.MESSAGE_TYPE_SERVICE == messageType) {
            InvokeHandler serviceInvokeHandler = serviceInvokeHandlers.get(rpcRole);
            if (serviceInvokeHandler == null) {
                synchronized (HandlerFactory.class) {
                    if (serviceInvokeHandler == null) {
                        serviceInvokeHandler = createServiceInvocationHandler(rpcRole);
                        serviceInvokeHandlers.putIfAbsent(rpcRole, serviceInvokeHandler);
                    }
                }
                serviceInvokeHandler = serviceInvokeHandlers.get(rpcRole);
            }
            return serviceInvokeHandler;
        } else {
            return getOtherInvocationHandler(messageType, rpcRole);
        }
    }

    protected abstract InvokeHandler getOtherInvocationHandler(int messageType, RpcRole rpcRole);

    private InvokeHandler createHeartBeatInvocationHandler() {
        InvokeHandler heartBeatInvokeHandler = ExtensionLoader
                .getExtension(HeartBeatInvokeHandler.class);

        return heartBeatInvokeHandler;
    }

    private InvokeHandler createServiceInvocationHandler(RpcRole role) {
        InvokeHandler serviceInvocationHandler = ExtensionLoader
                .getExtensionByRole(InvokeHandler.class, role);
        if (serviceInvocationHandler == null) {
            throw new IllegalStateException("Can not found InvokeHandler with role: " + role);
        }
        return serviceInvocationHandler;
    }
}
