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

import com.meituan.dorado.bootstrap.provider.ServicePublisher;
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.extension.ExtensionLoader;
import com.meituan.dorado.registry.RegistryFactory;
import com.meituan.dorado.rpc.handler.filter.Filter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class ProviderConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderConfig.class);

    protected String appkey;
    // mns, zookeeper://address?k=v&k=v; 没配置则从SPI中获取
    private String registry;

    // 协议
    private String protocol = Constants.ProtocolType.Thrift.getName();
    // 序列化类型, 默认thrift, 使用thrift时此配置无意义
    private String serialize = Constants.DEFAULT_SERIALIZE;
    // 端口
    private int port = Constants.DEFAULT_SERVER_PORT;
    // 网络IO线程数
    private int ioWorkerThreadCount = Constants.DEFAULT_IO_WORKER_THREAD_COUNT;
    // 业务线程池配置1: 参数
    private int bizCoreWorkerThreadCount = Constants.DEFAULT_BIZ_CORE_WORKER_THREAD_COUNT;
    private int bizMaxWorkerThreadCount = Constants.DEFAULT_BIZ_MAX_WORKER_THREAD_COUNT;
    private int bizWorkerQueueSize = Constants.DEFAULT_BIZ_WORKER_QUEUES;
    private BlockingQueue<Runnable> threadPoolQueue;

    // 单端口单服务
    private ServiceConfig serviceConfig;
    // 单端口多服务
    private List<ServiceConfig> serviceConfigList = new ArrayList<>();

    // 权重
    private Integer weight = Constants.DEFAULT_WEIGHT;
    // 预热时间 秒
    private Integer warmup = 0;
    // 分阶段耗时统计
    private boolean timelineTrace;
    // 兼容bean配置, 也可以SPI配置
    private List<Filter> filters = Collections.emptyList();
    private String env = Constants.EnvType.TEST.getEnvName();

    private volatile ShutDownHook hook;

    private volatile boolean destroyed;

    public void init() {
        check();
        if (StringUtils.isBlank(registry)) {
            RegistryFactory registryFactory = ExtensionLoader.getExtension(RegistryFactory.class);
            registry = registryFactory.getName();
        }
        if (serviceConfig != null) {
            serviceConfigList.add(serviceConfig);
        }
        for (ServiceConfig serviceConfig : serviceConfigList) {
            serviceConfig.check();
            serviceConfig.configTrace(appkey);
        }
        addShutDownHook();
        ServicePublisher.publishService(this);
    }

    protected void check() {
        if (bizCoreWorkerThreadCount > bizMaxWorkerThreadCount) {
            throw new IllegalArgumentException("bizCoreWorkerThreadCount must less than bizMaxWorkerThreadCount");
        } else if (bizCoreWorkerThreadCount == bizMaxWorkerThreadCount) {
            LOGGER.warn("bizCoreWorkerThreadCount equals to bizMaxWorkerThreadCount, it may cause many idle thread kept in pool.");
        }
    }

    public synchronized void destroy() {
        if (destroyed) {
            return;
        }
        ServicePublisher.unpublishService(this);
        destroyed = true;
    }

    protected synchronized void addShutDownHook() {
        if (hook == null) {
            hook = new ShutDownHook(this);
            Runtime.getRuntime().addShutdownHook(hook);
        }
    }

    public List<String> getServiceList() {
        List<String> serviceList = new ArrayList<>();
        for (ServiceConfig serviceConfig : serviceConfigList) {
            serviceList.add(serviceConfig.getServiceName());
        }
        return serviceList;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getSerialize() {
        return serialize;
    }

    public void setSerialize(String serialize) {
        this.serialize = serialize;
    }

    public String getRegistry() {
        return registry;
    }

    public void setRegistry(String registry) {
        this.registry = registry;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getAppkey() {
        return appkey;
    }

    public void setAppkey(String appkey) {
        this.appkey = appkey;
    }

    public int getIoWorkerThreadCount() {
        return ioWorkerThreadCount;
    }

    public void setIoWorkerThreadCount(int ioWorkerThreadCount) {
        this.ioWorkerThreadCount = ioWorkerThreadCount;
    }

    public void setServiceConfig(ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    public void setServiceConfigList(List<ServiceConfig> serviceConfigList) {
        this.serviceConfigList = serviceConfigList;
    }

    public List<ServiceConfig> getServiceConfigList() {
        return serviceConfigList;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public Integer getWarmup() {
        return warmup;
    }

    public void setWarmup(Integer warmup) {
        this.warmup = warmup;
    }

    public void setFilters(List<Filter> filters) {
        this.filters = filters;
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public boolean isTimelineTrace() {
        return timelineTrace;
    }

    public void setTimelineTrace(boolean timelineTrace) {
        this.timelineTrace = timelineTrace;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
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

    public BlockingQueue<Runnable> getThreadPoolQueue() {
        return threadPoolQueue;
    }

    public void setThreadPoolQueue(BlockingQueue<Runnable> threadPoolQueue) {
        this.threadPoolQueue = threadPoolQueue;
    }

    class ShutDownHook extends Thread {
        private ProviderConfig config;

        public ShutDownHook(ProviderConfig config) {
            this.config = config;
        }

        @Override
        public void run() {
            hook = null;
            config.destroy();
        }
    }
}
