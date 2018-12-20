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
package com.meituan.dorado.config.service;

import com.meituan.dorado.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ExecutorService;

public class ServiceConfig<T> extends AbstractConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceConfig.class);

    // 接口实现 必配项
    private T serviceImpl;

    // 业务线程池配置1: 参数
    private int bizCoreWorkerThreadCount = Constants.DEFAULT_BIZ_CORE_WORKER_THREAD_COUNT;
    private int bizMaxWorkerThreadCount = Constants.DEFAULT_BIZ_MAX_WORKER_THREAD_COUNT;
    private int bizWorkerQueueSize = Constants.DEFAULT_BIZ_WORKER_QUEUES;
    // 业务线程池配置2: 线程池对象
    private ExecutorService bizWorkerExecutor;
    // 业务线程池配置3: 方法粒度线程池对象
    private Map<String, ExecutorService> methodWorkerExecutors;

    /**
     * 服务端相关参数检查
     */
    protected void check() {
        if (serviceImpl == null) {
            throw new IllegalArgumentException("serviceImpl cannot be null");
        }
        if (serviceInterface == null) {
            Class[] interfaces = serviceImpl.getClass().getInterfaces();
            if (interfaces.length < 1) {
                throw new IllegalArgumentException("serviceImpl no interface");
            }
            serviceInterface = interfaces[0];
        }
        if (bizCoreWorkerThreadCount > bizMaxWorkerThreadCount) {
            throw new IllegalArgumentException("bizCoreWorkerThreadCount must less than bizMaxWorkerThreadCount");
        } else if (bizCoreWorkerThreadCount == bizMaxWorkerThreadCount) {
            LOGGER.warn("bizCoreWorkerThreadCount equals to bizMaxWorkerThreadCount, it may cause many idle thread keep in pool.");
        }

        serviceName = serviceInterface.getName();
        if (serviceName.indexOf(Constants.LINK_SUB_CLASS_SYMBOL) > 0) {
            serviceName = serviceName.substring(0, serviceName.indexOf(Constants.LINK_SUB_CLASS_SYMBOL));
        }
        if (!serviceInterface.isInterface()) {
            serviceInterface = getSynIfaceInterface(serviceInterface);
        }
        if (!getServiceInterface().isAssignableFrom(getServiceImpl().getClass())) {
            throw new IllegalArgumentException("serviceImpl must be sub class of class:"
                    + getServiceInterface() + ", serviceImpl: " + getServiceImpl());
        }
    }

    /**
     * Spring bean 该destroy会被执行两次
     */
    @Override
    public synchronized void destroy() {
    }

    public Object getServiceImpl() {
        return serviceImpl;
    }

    public void setServiceImpl(T serviceImpl) {
        this.serviceImpl = serviceImpl;
    }

    public int getBizCoreWorkerThreadCount() {
        return bizCoreWorkerThreadCount;
    }

    public void setBizCoreWorkerThreadCount(int bizCoreWorkerThreadCount) {
        this.bizCoreWorkerThreadCount = bizCoreWorkerThreadCount;
    }

    public int getBizMaxWorkerThreadCount() {
        return bizMaxWorkerThreadCount;
    }

    public void setBizMaxWorkerThreadCount(int bizMaxWorkerThreadCount) {
        this.bizMaxWorkerThreadCount = bizMaxWorkerThreadCount;
    }

    public int getBizWorkerQueueSize() {
        return bizWorkerQueueSize;
    }

    public void setBizWorkerQueueSize(int bizWorkerQueueSize) {
        this.bizWorkerQueueSize = bizWorkerQueueSize;
    }

    public ExecutorService getBizWorkerExecutor() {
        return bizWorkerExecutor;
    }

    public void setBizWorkerExecutor(ExecutorService bizWorkerExecutor) {
        this.bizWorkerExecutor = bizWorkerExecutor;
    }

    public Map<String, ExecutorService> getMethodWorkerExecutors() {
        return methodWorkerExecutors;
    }

    public void setMethodWorkerExecutors(Map<String, ExecutorService> methodWorkerExecutors) {
        this.methodWorkerExecutors = methodWorkerExecutors;
    }
}
