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
package com.meituan.dorado.cluster.tolerance;

import com.meituan.dorado.cluster.ClusterHandler;
import com.meituan.dorado.cluster.InvokerRepository;
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.thread.DefaultThreadFactory;
import com.meituan.dorado.rpc.handler.invoker.Invoker;
import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.rpc.meta.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 失败自动恢复，后台记录失败请求重选节点定时重发，通常用于消息通知操作
 */
public class FailbackClusterHandler<T> extends ClusterHandler<T> {

    private static final Logger logger = LoggerFactory.getLogger(FailbackClusterHandler.class);

    private static final long RETRY_FAILED_PERIOD = 5 * 1000L;

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2, new DefaultThreadFactory("failback-cluster-timer", true));
    private final ConcurrentMap<RpcInvocation, ClusterHandler<?>> failed = new ConcurrentHashMap<RpcInvocation, ClusterHandler<?>>();
    private volatile ScheduledFuture<?> retryFuture;

    public FailbackClusterHandler(InvokerRepository<T> repository) {
        super(repository);
    }

    @Override
    protected RpcResult doInvoke(RpcInvocation invocation, List<Invoker<T>> invokers) {
        Invoker<T> invoker = select(invocation, invokers, new ArrayList<Invoker<T>>());
        try {
            return invoker.invoke(invocation);
        } catch (Throwable e) {
            logger.error("Remote invoke failed, interface=" + invoker.getInterface().getName() +
                    "|method=" + invocation.getMethod().getName() + "|provider=" +
                    invoker.getProvider().getIp() + Constants.COLON + invoker.getProvider().getPort() + ", wait for retry in background", e);
            addFailed(invocation, this);
        }
        return new RpcResult();
    }

    private void addFailed(RpcInvocation invocation, ClusterHandler<T> clusterHandler) {
        if (retryFuture == null) {
            synchronized (this) {
                if (retryFuture == null) {
                    retryFuture = scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
                        @Override
                        public void run() {
                            // 收集统计信息
                            try {
                                retryFailed();
                            } catch (Throwable t) {
                                logger.error("Unexpected error occur at retry", t);
                            }
                        }
                    }, RETRY_FAILED_PERIOD, RETRY_FAILED_PERIOD, TimeUnit.MILLISECONDS);
                }
            }
        }
        failed.put(invocation, clusterHandler);
    }

    private void retryFailed() {
        if (failed.size() == 0) {
            return;
        }
        for (Map.Entry<RpcInvocation, ClusterHandler<?>> entry : new HashMap<>(failed).entrySet()) {
            RpcInvocation invocation = entry.getKey();
            ClusterHandler clusterHandler = entry.getValue();
            try {
                clusterHandler.handle(invocation);
                failed.remove(invocation);
            } catch (Throwable e) {
                logger.error("Failed retry to invoke method=" + invocation.getMethod().getName() + ", waiting again.", e);
            }
        }
    }
}
