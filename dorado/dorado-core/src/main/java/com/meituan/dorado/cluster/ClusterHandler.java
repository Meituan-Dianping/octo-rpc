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
package com.meituan.dorado.cluster;

import com.meituan.dorado.cluster.loadbalance.LoadBalanceFactory;
import com.meituan.dorado.cluster.router.RouterFactory;
import com.meituan.dorado.common.exception.RegistryException;
import com.meituan.dorado.common.exception.RpcException;
import com.meituan.dorado.rpc.handler.invoker.Invoker;
import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.rpc.meta.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public abstract class ClusterHandler<T> {

    private static final Logger logger = LoggerFactory.getLogger(ClusterHandler.class);

    protected Class<T> serviceInterface;
    private InvokerRepository<T> repository;
    private LoadBalance loadBalance;

    public ClusterHandler(InvokerRepository<T> repository) {
        this.repository = repository;
        this.serviceInterface = repository.getServiceInterface();
    }

    public Class<T> getInterface() {
        return serviceInterface;
    }

    public RpcResult handle(RpcInvocation invocation) throws Throwable {
        Router router = RouterFactory.getRouter(repository.getClientConfig().getServiceName());
        loadBalance = LoadBalanceFactory.getLoadBalance(repository.getClientConfig().getServiceName());

        List<Invoker<T>> invokerList = obtainInvokers();
        List<Invoker<T>> invokersAfterRoute = router.route(invokerList);
        if (invokersAfterRoute == null || invokersAfterRoute.isEmpty()) {
            logger.error("Provider list is empty after route, router policy:{}, ignore router policy", router.getName());
        } else {
            invokerList = invokersAfterRoute;
        }
        return doInvoke(invocation, invokerList);
    }

    protected abstract RpcResult doInvoke(final RpcInvocation invocation, List<Invoker<T>> invokers) throws Throwable;

    public void destroy() {
        repository.destroy();
    }

    public InvokerRepository<T> getRepository() {
        return repository;
    }

    protected Invoker<T> select(RpcInvocation invocation, List<Invoker<T>> invokers, List<Invoker<T>> invoked) {
        if (invokers == null || invokers.isEmpty()) {
            // 不应该到此逻辑, obtainInvokers已抛出异常
            throw new RpcException("No invokers, check if there are usable server node");
        }
        if (invokers.size() == 1) {
            return invokers.get(0);
        }
        if (invokers.size() == 2 && !invoked.isEmpty()) {
            return invoked.get(0) == invokers.get(0) ? invokers.get(1) : invokers.get(0);
        }

        Invoker<T> invoker;
        // 已调用过的节点, 则重新选择
        if (!invoked.isEmpty() && invokers.size() > invoked.size()) {
            invoker = selectNoInvoked(invokers, invoked);
            if (invoker == null) {
                invoker = loadBalance.select(invokers);
            }
        } else {
            invoker = loadBalance.select(invokers);
        }
        return invoker;
    }

    protected Invoker<T> selectNoInvoked(List<Invoker<T>> invokers, List<Invoker<T>> invoked) {
        List<Invoker<T>> noInvokedList = new ArrayList<Invoker<T>>();

        for (Invoker invoker : invokers) {
            if (!invoked.contains(invoker)) {
                noInvokedList.add(invoker);
            }
        }
        if (!noInvokedList.isEmpty()) {
            return loadBalance.select(noInvokedList);
        }
        return null;
    }

    protected List<Invoker<T>> obtainInvokers() {
        List<Invoker<T>> invokers = repository.getInvokers();
        if (invokers == null || invokers.isEmpty()) {
            throw new RegistryException("Provider list is empty, check if there are usable server node. remoteAppkey="
                    + repository.getClientConfig().getRemoteAppkey());
        }
        return invokers;
    }
}
