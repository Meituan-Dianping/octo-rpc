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
package com.meituan.dorado;


import com.meituan.dorado.cluster.InvokerRepository;
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.exception.RpcException;
import com.meituan.dorado.config.service.ClientConfig;
import com.meituan.dorado.config.service.ProviderConfig;
import com.meituan.dorado.config.service.ReferenceConfig;
import com.meituan.dorado.config.service.ServiceConfig;
import com.meituan.dorado.config.service.spring.ReferenceBean;
import com.meituan.dorado.config.service.spring.ServiceBean;
import com.meituan.dorado.mock.*;
import com.meituan.dorado.registry.RegistryPolicy;
import com.meituan.dorado.registry.meta.Provider;
import com.meituan.dorado.registry.meta.RegistryInfo;
import com.meituan.dorado.registry.meta.SubscribeInfo;
import com.meituan.dorado.rpc.DefaultFuture;
import com.meituan.dorado.rpc.ResponseCallback;
import com.meituan.dorado.rpc.ResponseFuture;
import com.meituan.dorado.rpc.handler.filter.FilterHandler;
import com.meituan.dorado.rpc.handler.invoker.Invoker;
import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.rpc.meta.RpcResult;
import com.meituan.dorado.transport.Channel;
import com.meituan.dorado.transport.meta.Request;
import com.meituan.dorado.transport.meta.Response;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MockUtil {

    public static ReferenceConfig getReferenceConfig() {
        ReferenceConfig config = new ReferenceConfig();
        config.setAppkey("com.meituan.octo.dorado.client");
        config.setRemoteAppkey("com.meituan.octo.dorado.server");
        config.setServiceInterface(HelloService.class);
        config.setRegistry("mock://127.0.0.1:9000");
        config.setServiceName("HelloService");
        config.setProtocol("mock");
        config.setClusterPolicy("mock");
        config.setDirectConnAddress("1.2.3.4:5");
        return config;
    }

    public static ProviderConfig getProviderConfig() {
        ServiceConfig  serviceConfig = getServiceConfig();
        ProviderConfig config = new ProviderConfig();
        config.setPort(9001);
        config.setAppkey("com.meituan.octo.dorado.server");
        config.getServiceConfigList().add(serviceConfig);
        config.getServiceConfigList().add(serviceConfig);
        return config;
    }

    public static ServiceConfig getServiceConfig() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setServiceImpl(new HelloServiceImpl());
        serviceConfig.setServiceInterface(HelloService.class);
        serviceConfig.setServiceName(HelloService.class.getName());
        return serviceConfig;
    }

    public static MockInvoker getInvoker() {
        Provider provider = getProvider();
        ClientConfig clientConfig = getClientConfig();
        MockInvoker invoker = new MockInvoker(provider, HelloService.class, clientConfig);
        return invoker;
    }

    public static Provider getProvider() {
        Provider provider = new Provider();
        provider.setWeight(10);
        provider.setIp("1.2.3.4");
        provider.setPort(1000);
        return provider;
    }

    public static RpcInvocation getRpcInvocation() throws NoSuchMethodException {
        Method method = HelloService.class.getMethod("sayHello", String.class);
        RpcInvocation invocation = new RpcInvocation(HelloService.class, method,
                new Object[] {"hello"}, new Class<?>[] {String.class});
        invocation.putAttachment(Constants.RPC_REQUEST, getRequest());
        return invocation;
    }

    public static RpcInvocation getRpcInvocationWithoutRequest() throws NoSuchMethodException {
        Method method = HelloService.class.getMethod("sayHello", String.class);
        RpcInvocation invocation = new RpcInvocation(HelloService.class, method,
                new Object[] {"hello"}, new Class<?>[] {String.class});
        return invocation;
    }

    public static Invoker getErrorInvoker() {
        ClientConfig clientConfig = getClientConfig();
        Provider provider = getProvider();

        MockErrorInvoker invoker = new MockErrorInvoker(provider, HelloService.class, clientConfig);
        return invoker;
    }

    public static ClientConfig getClientConfig() {
        ReferenceConfig config = getReferenceConfig();
        ClientConfig clientConfig = new ClientConfig(config);
        clientConfig.setAddress("127.0.0.1", 9000);
        return clientConfig;
    }

    public static<T> List<Invoker<T>> getInvokerList() {
        List<Invoker<T>> invokers = new ArrayList<>();
        invokers.add(MockUtil.getInvoker());

        Invoker invoker = MockUtil.getInvoker();
        invoker.getProvider().setWeight(15);
        invokers.add(invoker);

        invoker = MockUtil.getInvoker();
        invoker.getProvider().setWeight(20);
        invokers.add(invoker);

        invoker = MockUtil.getInvoker();
        invoker.getProvider().setWeight(30);
        invokers.add(invoker);

        return invokers;
    }

    public static List<Provider> getProviderList() {
        List<Provider> providerList = new ArrayList<>();

        Provider provider = getProvider();
        providerList.add(provider);

        provider = getProvider();
        provider.setIp("1.1.1.1");
        providerList.add(provider);

        provider = getProvider();
        provider.setIp("2.2.2.2");
        providerList.add(provider);
        return providerList;
    }

    public static FilterHandler getFilterHandler() {
        MockFilterHandler handler = new MockFilterHandler();
        return handler;
    }

    public static Request getRequest() throws NoSuchMethodException {
        MockRequest request = new MockRequest();
        request.setData(getRpcInvocationWithoutRequest());
        return request;
    }

    public static Response getResponse() {
        MockResponse response = new MockResponse();
        RpcResult rpcResult = new RpcResult();
        rpcResult.setReturnVal(5);
        response.setResult(rpcResult);
        return response;
    }

    public static Response getErrorResponse() {
        MockResponse response = new MockResponse();
        RpcResult rpcResult = new RpcResult();
        response.setException(new RpcException("Mock Exception."));
        return response;
    }

    public static Channel getChannel() {
        MockChannel channel = new MockChannel();
        return channel;
    }

    public static ResponseFuture<Integer> getFuture() throws NoSuchMethodException {
        Request request = getRequest();
        Channel channel = getChannel();
        ResponseFuture<Integer> future = new DefaultFuture<>(request, channel, 1000);
        future.setCallback(getCallback());
        return future;
    }

    public static InvokerRepository getInvokerRepository() {
        InvokerRepository repository = new InvokerRepository<>(getClientConfig());
        repository.getInvokers().addAll(getInvokerList());
        return repository;
    }

    public static InvokerRepository getEmptyInvokerRepository() {
        InvokerRepository repository = new InvokerRepository<>(getClientConfig());
        return repository;
    }

    public static ReferenceBean getReferenceBean() {
        ReferenceBean<HelloService> bean = new ReferenceBean<>();
        bean.setServiceInterface(HelloService.class);
        bean.setAppkey("com.meituan.octo.dorado.client");
        bean.setRemoteAppkey("com.meituan.octo.dorado.server");
        bean.setServiceInterface(HelloService.class);
        bean.setRegistry("mock://127.0.0.1:9000");
        bean.setServiceName("HelloService");
        bean.setProtocol("mock");
        bean.setClusterPolicy("mock");
        bean.setDirectConnAddress("1.2.3.4:5");
        return bean;
    }

    public static ServiceBean getServerBean() {
        ServiceConfig  serviceConfig = getServiceConfig();
        ServiceBean bean = new ServiceBean();

        bean.setPort(9001);
        bean.setAppkey("com.meituan.octo.dorado.server");
        bean.getServiceConfigList().add(serviceConfig);
        bean.getServiceConfigList().add(serviceConfig);
        return bean;
    }

    public static MockRegistryPolicy getRegistryPolicy() {
        MockRegistryPolicy policy = new MockRegistryPolicy(new MockRegistry());
        return policy;
    }

    public static RegistryInfo getRegistryInfo() {
        RegistryInfo info = new RegistryInfo();
        info.setServiceNames(new ArrayList<String>());
        return info;
    }

    public static SubscribeInfo getSubsribeInfo() {
        SubscribeInfo info = new SubscribeInfo();
        return info;
    }

    public static MockClient getClient() {
        MockClient client = new MockClient(getClientConfig(), getInvoker());
        return client;
    }

    public static MockServer getServer() {
        MockServer server = new MockServer(getProviderConfig());
        return server;
    }

    public static ResponseCallback getCallback() {
        return new ResponseCallback() {

            @Override
            public void onComplete(Object result) {}

            @Override
            public void onError(Throwable e) {}
        };
    }
}
