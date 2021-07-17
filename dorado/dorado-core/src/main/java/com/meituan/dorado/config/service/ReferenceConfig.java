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

import com.meituan.dorado.bootstrap.invoker.ServiceSubscriber;
import com.meituan.dorado.cluster.ClusterHandler;
import com.meituan.dorado.cluster.LoadBalance;
import com.meituan.dorado.cluster.loadbalance.LoadBalanceFactory;
import com.meituan.dorado.cluster.router.RouterFactory;
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.common.exception.RpcException;
import com.meituan.dorado.common.extension.ExtensionLoader;
import com.meituan.dorado.config.service.util.CallWayEnum;
import com.meituan.dorado.registry.RegistryFactory;
import com.meituan.dorado.rpc.handler.filter.Filter;
import com.meituan.dorado.rpc.proxy.ProxyFactory;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ReferenceConfig<T> extends AbstractConfig implements Disposable {

    // 必配项 服务端 appkey
    private String remoteAppkey;
    private String appkey;

    // ---连接相关---
    // mns, zookeeper://address
    private String registry;
    // 直连配置ip:port, 可配置多个"," 分隔
    private String directConnAddress;
    // 用于直连时 是否使用统一协议的配置
    private boolean remoteOctoProtocol;

    private String callWay = CallWayEnum.SYNC.getName();

    // 集群容错策略
    private String clusterPolicy = Constants.DEFAULT_CLUSTER_POLICY;
    // 只有failover策略生效
    private int failoverRetryTimes = 3;

    // 负载均衡
    private LoadBalance customLoadBalance;
    private String loadBalancePolicy = Constants.DEFAULT_LOADBALANCE_POLICY;
    // 路由
    private String routerPolicy = Constants.DEFAULT_ROUTER_POLICY;

    // 协议
    private String protocol = Constants.ProtocolType.Thrift.getName();
    // 序列化类型, 默认thrift, 使用thrift时此配置无意义
    private String serialize = Constants.DEFAULT_SERIALIZE;

    // 超时设置
    private int connTimeout = Constants.DEFAULT_CONN_TIMEOUT;
    private int timeout = Constants.DEFAULT_TIMEOUT;
    private Map<String, Integer> methodTimeout = Collections.emptyMap();

    // 分阶段耗时统计
    private boolean timelineTrace = false;
    // 兼容bean配置, 也可以SPI配置
    private List<Filter> filters = Collections.emptyList();

    private String env = Constants.EnvType.TEST.getEnvName();

    // 接口代理类
    private transient volatile T proxyObj;
    private transient ClusterHandler<?> clusterHandler;

    private volatile boolean destroyed;

    public synchronized T get() {
        if (destroyed) {
            throw new IllegalStateException("Proxy obj has been destroyed!");
        }
        if (proxyObj == null) {
            init();
        }
        return proxyObj;
    }

    /**
     * Spring bean该destroy会被执行两次
     */
    @Override
    public synchronized void destroy() {
        if (destroyed) {
            return;
        }
        ServiceSubscriber.unsubscribeService(this);
        clusterHandler = null;
        proxyObj = null;
        destroyed = true;
    }

    public synchronized void init() {
        check();

        configLoadBalance();
        configRouter();
        configTrace(appkey);
        clusterHandler = ServiceSubscriber.subscribeService(this);

        ProxyFactory proxyFactory = ExtensionLoader.getExtension(ProxyFactory.class);
        proxyObj = (T) proxyFactory.getProxy(clusterHandler);
        loadAndInitExtensionList(this, RpcRole.INVOKER);
        addShutDownHook();
    }

    private void configLoadBalance() {
        try {
            if (customLoadBalance != null) {
                LoadBalanceFactory.setCustomLoadBalance(serviceName, customLoadBalance);
            } else {
                LoadBalanceFactory.setLoadBalance(serviceName, loadBalancePolicy);
            }
        } catch (Exception e) {
            throw new RpcException(e.getMessage(), e);
        }
    }

    private void configRouter() {
        RouterFactory.setRouter(serviceName, routerPolicy);
    }

    protected synchronized void addShutDownHook() {
        ShutdownHook.register(this);
    }

    /**
     * 调用端相关参数检查
     */
    protected void check() {
        if (serviceInterface == null) {
            throw new IllegalArgumentException("serviceInterface cannot be null");
        }
        if (appkey == null) {
            throw new IllegalArgumentException("appkey cannot be null");
        }
        serviceName = serviceInterface.getName();
        if (!serviceInterface.isInterface()) {
            serviceInterface = getSyncIfaceInterface(serviceInterface);
        }

        if (StringUtils.isBlank(registry)) {
            RegistryFactory registryFactory = ExtensionLoader.getExtension(RegistryFactory.class);
            registry = registryFactory.getName();
        } else if (Constants.REGISTRY_MOCK_WAY.equalsIgnoreCase(registry) && StringUtils.isBlank(directConnAddress)) {
            throw new IllegalArgumentException("mock registry way must be used with directConnAddress.");
        }

        if (remoteAppkey == null) {
            throw new IllegalArgumentException("remoteAppkey cannot be null");
        }

        if (directConnAddress != null && !directConnAddress.contains(Constants.COLON)) {
            throw new IllegalArgumentException("remoteAddress config invalid, no port");
        }

        Method[] methods = serviceInterface.getDeclaredMethods();
        List<String> methodNameList = new ArrayList<>();
        for (Method method : methods) {
            methodNameList.add(method.getName());
        }
        for (String methodName : methodTimeout.keySet()) {
            if (!methodNameList.contains(methodName)) {
                throw new IllegalArgumentException("Service does not contain method " + methodName);
            }
        }
    }

    public ClusterHandler<?> getClusterHandler() {
        return clusterHandler;
    }

    public String getRemoteAppkey() {
        return remoteAppkey;
    }

    public void setRemoteAppkey(String remoteAppkey) {
        this.remoteAppkey = remoteAppkey;
    }

    public String getRegistry() {
        return registry;
    }

    public void setRegistry(String registry) {
        this.registry = registry;
    }

    public String getDirectConnAddress() {
        return directConnAddress;
    }

    public void setDirectConnAddress(String directConnAddress) {
        this.directConnAddress = directConnAddress;
    }

    public boolean isRemoteOctoProtocol() {
        return remoteOctoProtocol;
    }

    public void setRemoteOctoProtocol(boolean remoteOctoProtocol) {
        this.remoteOctoProtocol = remoteOctoProtocol;
    }

    public String getCallWay() {
        return callWay;
    }

    public void setCallWay(String callWay) {
        this.callWay = callWay;
    }

    public String getClusterPolicy() {
        return clusterPolicy;
    }

    public void setClusterPolicy(String clusterPolicy) {
        this.clusterPolicy = clusterPolicy;
    }

    public int getFailoverRetryTimes() {
        return failoverRetryTimes;
    }

    public void setFailoverRetryTimes(int failoverRetryTimes) {
        this.failoverRetryTimes = failoverRetryTimes;
    }

    public LoadBalance getCustomLoadBalance() {
        return customLoadBalance;
    }

    public void setCustomLoadBalance(LoadBalance customLoadBalance) {
        this.customLoadBalance = customLoadBalance;
    }

    public String getLoadBalancePolicy() {
        return loadBalancePolicy;
    }

    public void setLoadBalancePolicy(String loadBalancePolicy) {
        this.loadBalancePolicy = loadBalancePolicy;
    }

    public String getRouterPolicy() {
        return routerPolicy;
    }

    public void setRouterPolicy(String routerPolicy) {
        this.routerPolicy = routerPolicy;
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

    public int getConnTimeout() {
        return connTimeout;
    }

    public void setConnTimeout(int connTimeout) {
        this.connTimeout = connTimeout;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getAppkey() {
        return appkey;
    }

    public void setAppkey(String appkey) {
        this.appkey = appkey;
    }

    public Map<String, Integer> getMethodTimeout() {
        return methodTimeout;
    }

    public void setMethodTimeout(Map<String, Integer> methodTimeout) {
        this.methodTimeout = methodTimeout;
    }

    public boolean isTimelineTrace() {
        return timelineTrace;
    }

    public void setTimelineTrace(boolean timelineTrace) {
        this.timelineTrace = timelineTrace;
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public void setFilters(List<Filter> filters) {
        this.filters = filters;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

}
