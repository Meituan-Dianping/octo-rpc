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
package com.meituan.dorado.rpc.handler.invoker;

import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.common.exception.RpcException;
import com.meituan.dorado.common.exception.TimeoutException;
import com.meituan.dorado.common.extension.ExtensionLoader;
import com.meituan.dorado.config.service.ClientConfig;
import com.meituan.dorado.registry.meta.Provider;
import com.meituan.dorado.rpc.AsyncContext;
import com.meituan.dorado.rpc.handler.HandlerFactory;
import com.meituan.dorado.rpc.handler.InvokeHandler;
import com.meituan.dorado.rpc.handler.filter.FilterHandler;
import com.meituan.dorado.rpc.handler.filter.InvokeChainBuilder;
import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.rpc.meta.RpcResult;
import com.meituan.dorado.transport.Client;
import com.meituan.dorado.transport.ClientFactory;
import com.meituan.dorado.transport.meta.Request;
import com.meituan.dorado.transport.meta.Response;

public abstract class AbstractInvoker<T> implements Invoker<T> {

    protected Class<T> serviceInterface;

    protected Provider provider;

    protected ClientConfig config;

    // 可能多连接, 交由Client连接池
    protected Client client;

    protected FilterHandler handler;

    protected volatile boolean destroyed;

    public AbstractInvoker() {}

    public AbstractInvoker(ClientConfig config, Provider provider) {
        this.serviceInterface = (Class<T>) config.getServiceInterface();
        this.provider = provider;
        this.config = config;

        initClient();
        FilterHandler actualHandler = buildActualInvokeHandler();
        handler = InvokeChainBuilder.initInvokeChain(actualHandler, config.getFilters(), RpcRole.INVOKER);
    }

    @Override
    public RpcResult invoke(RpcInvocation invocation) throws Throwable {
        Request request = genRequest();
        request.setData(invocation);
        invocation.putAttachment(Constants.RPC_REQUEST, request);
        invocation.putAttachments(AsyncContext.getContext().getAttachments());

        return handler.handle(invocation);
    }

    @Override
    public boolean updateProviderIfNeeded(Provider provider) {
        if (this.provider.getIp().equals(provider.getIp()) &&
                this.provider.getPort() == provider.getPort()) {
            // 更新字段内容，不直接替换引用的原因是避免节点过多时替换较多，对ygc耗时有轻微影响，目的是减少对象在年轻代的复制
            return this.provider.updateIfDiff(provider);
        }
        return false;
    }

    @Override
    public void destroy() {
        synchronized (this) {
            if (client != null) {
                client.close();
                client = null;
            }
            destroyed = true;
        }
    }

    @Override
    public Client getClient() {
        return client;
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    private FilterHandler buildActualInvokeHandler() {
        final InvokeHandler invokerHandler = ExtensionLoader.getExtension(HandlerFactory.class)
                .getInvocationHandler(Constants.MESSAGE_TYPE_SERVICE, RpcRole.INVOKER);
        FilterHandler serviceInvokeHandler = new FilterHandler() {
            @Override
            public RpcResult handle(RpcInvocation invocation) throws Throwable {
                try {
                    Request request = (Request)invocation.getAttachment(Constants.RPC_REQUEST);
                    if (request == null) {
                        throw new RpcException("No request info in RpcInvocation");
                    }

                    String methodName = invocation.getMethod().getName();
                    Integer timeout = config.getMethodTimeout().get(methodName);
                    if (timeout != null) {
                        request.setTimeout(timeout);
                    } else {
                        request.setTimeout(config.getTimeout());
                    }
                    request.setData(invocation);

                    Response response = invokerHandler.handle(request);
                    if (response.getException() != null) {
                        throw response.getException();
                    }
                    return response.getResult();
                } catch (Exception e) {
                    if (e instanceof TimeoutException) {
                        throw new TimeoutException(e.getMessage() + ", interface=" + serviceInterface.getName() +
                                "|method=" + invocation.getMethod().getName() + "|provider=" + provider.getIp() + Constants.COLON + provider.getPort(), e.getCause());
                    } else {
                        throw e;
                    }
                }
            }
        };
        return serviceInvokeHandler;
    }

    private void initClient() {
        ClientFactory clientFactory = ExtensionLoader.getExtension(ClientFactory.class);
        client = clientFactory.buildClient(config, this);
    }

    @Override
    public Class<T> getInterface() {
        return serviceInterface;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }
}
