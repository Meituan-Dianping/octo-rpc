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
package com.meituan.dorado.transport.support;

import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.common.exception.RpcException;
import com.meituan.dorado.common.exception.ServiceException;
import com.meituan.dorado.common.extension.ExtensionLoader;
import com.meituan.dorado.common.thread.DefaultThreadFactory;
import com.meituan.dorado.common.thread.ExecutorUtil;
import com.meituan.dorado.config.service.ProviderConfig;
import com.meituan.dorado.config.service.ServiceConfig;
import com.meituan.dorado.rpc.handler.HandlerFactory;
import com.meituan.dorado.rpc.handler.InvokeHandler;
import com.meituan.dorado.rpc.handler.filter.FilterHandler;
import com.meituan.dorado.rpc.handler.filter.InvokeChainBuilder;
import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.rpc.meta.RpcResult;
import com.meituan.dorado.trace.AbstractInvokeTrace;
import com.meituan.dorado.transport.Channel;
import com.meituan.dorado.transport.ChannelHandler;
import com.meituan.dorado.transport.meta.Request;
import com.meituan.dorado.transport.meta.Response;
import com.meituan.dorado.util.ClazzUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 每个端口服务的channel handler
 */
public class ProviderChannelHandler implements ChannelHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProviderChannelHandler.class);

    private static final RpcRole role = RpcRole.PROVIDER;

    private boolean destroyed;
    protected FilterHandler filterHandler;

    private HandlerFactory handlerFactory = ExtensionLoader.getExtension(HandlerFactory.class);

    private final Map<String, Class<?>> serviceInterfaceMap = new HashMap<>();
    private final Map<String, Object> serviceImplMap = new HashMap<>();

    private ThreadPoolExecutor defaultExecutor;

    private final Map<String, ExecutorService> serviceExecutorMap = new HashMap<>();
    private final Map<String, Map<String, ExecutorService>> methodExecutorMap = new HashMap<>();

    public ProviderChannelHandler(ProviderConfig providerConfig) {
        for (ServiceConfig serviceConfig : providerConfig.getServiceConfigList()) {
            serviceInterfaceMap.put(serviceConfig.getServiceName(), serviceConfig.getServiceInterface());
            serviceImplMap.put(serviceConfig.getServiceName(), serviceConfig.getServiceImpl());
            initThreadPoolExecutor(serviceConfig);
        }
        initDefaultThreadPool(providerConfig);

        FilterHandler actualHandler = buildActualInvokeHandler();
        filterHandler = InvokeChainBuilder.initInvokeChain(actualHandler, providerConfig.getFilters(), RpcRole.PROVIDER);
    }

    private void initDefaultThreadPool(ProviderConfig providerConfig) {
        DefaultThreadFactory threadFactory = new DefaultThreadFactory(genServerBizThreadPoolName(providerConfig));
        defaultExecutor = ExecutorUtil.getThreadPool(providerConfig.getBizCoreWorkerThreadCount(),
                providerConfig.getBizMaxWorkerThreadCount(),
                providerConfig.getBizWorkerQueueSize(),
                providerConfig.getThreadPoolQueue(),
                threadFactory);
        defaultExecutor.prestartAllCoreThreads();
    }

    private String genServerBizThreadPoolName(ProviderConfig providerConfig) {
        String bizThreadPoolName = "DoradoServerBizWorker-";
        if (providerConfig.getServiceConfigList().size() == 1) {
            bizThreadPoolName += providerConfig.getServiceConfigList().get(0).getServiceName();
        } else {
            bizThreadPoolName += providerConfig.getPort();
        }
        return bizThreadPoolName;
    }

    @Override
    public void received(final Channel channel, final Object message) {
        if (message instanceof Request) {
            final int messageType = ((Request) message).getMessageType();
            final InvokeHandler handler = handlerFactory.getInvocationHandler(messageType, role);
            try {
                prepareReqContext(channel, (Request) message);
                ExecutorService executor = getExecutor(messageType, message);
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        Response response = null;
                        try {
                            if (Constants.MESSAGE_TYPE_SERVICE == messageType) {
                                RpcInvocation invocation = ((Request) message).getData();
                                Request request = (Request) message;
                                Object serviceImpl = serviceImplMap.get(request.getServiceName());
                                if (serviceImpl == null) {
                                    throw new ServiceException("Not find serviceImpl by serviceName=" + request.getServiceName());
                                }
                                request.setServiceImpl(serviceImpl);
                                invocation.putAttachment(Constants.RPC_REQUEST, message);

                                RpcResult rpcResult = filterHandler.handle(invocation);
                                response = handler.buildResponse((Request) message);
                                response.setResult(rpcResult);
                            } else {
                                response = handler.handle(((Request) message));
                            }
                            send(channel, response);
                        } catch (Throwable e) {
                            if (Constants.MESSAGE_TYPE_SERVICE == messageType) {
                                logger.error("Provider do method invoke failed, {}:{}", e.getClass().getSimpleName(), e.getMessage(), e);
                                AbstractInvokeTrace.reportServerTraceInfoIfNeeded((Request) message, response);
                            } else {
                                logger.warn("Message handle failed, {}:{} ", e.getClass().getSimpleName(), e.getMessage());
                            }
                            boolean isSendFailedResponse = channel.isConnected() &&
                                    !(e.getCause() != null && e.getCause() instanceof ClosedChannelException);
                            if (isSendFailedResponse) {
                                sendFailedResponse(channel, handler, message, e);
                            }
                        }
                    }
                });
            } catch (RejectedExecutionException e) {
                sendFailedResponse(channel, handler, message, e);
                logger.error("Worker task is rejected", e);
            }
        } else {
            logger.warn("Should not reach here, it means your message({}) is ignored", message);
        }
    }

    @Override
    public void send(Channel channel, Object message) {
        channel.send(message);
    }

    /**
     * server销毁时调用
     */
    @Override
    public void destroy() {
        if (destroyed) {
            return;
        }

        List<ExecutorService> executors = new ArrayList<>();
        executors.addAll(serviceExecutorMap.values());
        for (Map<String, ExecutorService> executor : methodExecutorMap.values()) {
            executors.addAll(executor.values());
        }
        ExecutorUtil.shutdownExecutors(executors, Constants.DEFAULT_SHUTDOWN_TIMEOUT);
        destroyed = true;
    }

    private FilterHandler buildActualInvokeHandler() {
        final InvokeHandler invokeHandler = ExtensionLoader.getExtension(HandlerFactory.class)
                .getInvocationHandler(Constants.MESSAGE_TYPE_SERVICE, role);
        FilterHandler serviceInvokeHandler = new FilterHandler() {
            @Override
            public RpcResult handle(RpcInvocation invocation) throws Throwable {
                Request request = (Request) invocation.getAttachment(Constants.RPC_REQUEST);
                if (request == null) {
                    throw new RpcException("No request info in RpcInvocation");
                }
                Response response = invokeHandler.handle(request);
                if (response.getException() != null) {
                    throw response.getException();
                }
                return response.getResult();
            }
        };
        return serviceInvokeHandler;
    }

    private void initThreadPoolExecutor(ServiceConfig config) {
        if (config.getMethodWorkerExecutors() != null && !config.getMethodWorkerExecutors().isEmpty()) {
            Map<String, ExecutorService> methodExecutor = config.getMethodWorkerExecutors();
            methodExecutorMap.put(config.getServiceName(), methodExecutor);
        }

        if (config.getBizWorkerExecutor() != null) {
            ExecutorService executorService = config.getBizWorkerExecutor();
            serviceExecutorMap.put(config.getServiceName(), executorService);
        }
    }

    private ExecutorService getExecutor(int messageType, Object message) {
        ExecutorService executor = null;
        if (Constants.MESSAGE_TYPE_SERVICE == messageType) {
            RpcInvocation invocation = ((Request) message).getData();
            if (invocation == null) {
                return defaultExecutor;
            }
            String serviceName = extractServiceInterface(invocation.getServiceInterface()).getName();
            Map<String, ExecutorService> executorMap = methodExecutorMap.get(serviceName);
            if (executorMap != null) {
                String methodName = invocation.getMethod().getName();
                executor = executorMap.get(methodName);
            }
            if (executor == null) {
                executor = serviceExecutorMap.get(serviceName);
            }
        }
        if (executor == null) {
            executor = defaultExecutor;
        }
        return executor;
    }

    private Class<?> extractServiceInterface(Class<?> iface) {
        // IDL使用方式传入的iface是com.**.**$Iface, 需要获取实际的接口类即Iface的外部类
        if (ClazzUtil.isMemberClazz(iface)) {
            return ClazzUtil.getEnclosingClass(iface);
        }
        return iface;
    }

    private void prepareReqContext(Channel channel, Request request) {
        request.putAttachment(Constants.LOCAL_IP, channel.getLocalAddress().getHostName());
        request.putAttachment(Constants.LOCAL_PORT, channel.getLocalAddress().getPort());
        if (request.getRemoteAddress() == null) {
            request.setRemoteAddress(channel.getRemoteAddress());
        }
    }

    private void sendFailedResponse(final Channel channel, final InvokeHandler handler, Object message, Throwable e) {
        if (!channel.isConnected()) {
            return;
        }
        Response response = handler.buildResponse((Request) message);
        RpcResult rpcResult = new RpcResult();
        rpcResult.setReturnVal(e);
        response.setResult(rpcResult);
        response.setException(e);

        send(channel, response);
    }

    private boolean checkIfServiceSupport(final int messageType, String serviceName) {
        return this.serviceInterfaceMap.keySet().contains(serviceName);
    }

    @Override
    public void exceptionCaught(Channel channel, Throwable exception) {
        String clientIP = channel.getRemoteAddress().getHostName();
        String message = exception.getMessage();
        if (message != null && message.contains(Constants.NORMAL_DISCONNCT_INFO)) {
            logger.warn("ExceptionCaught(client IP:{}): {}", clientIP, message);
        } else {
            logger.error("ExceptionCaught(client IP:{})", clientIP, exception);
        }
    }

    @Override
    public void connected(Channel channel) {
        // do nothing
    }

    @Override
    public void disconnected(Channel channel) {
        // do nothing
    }

    @Override
    public void closed(Channel channel) {
        // do nothing , channel will be closed in transport module
    }
}
