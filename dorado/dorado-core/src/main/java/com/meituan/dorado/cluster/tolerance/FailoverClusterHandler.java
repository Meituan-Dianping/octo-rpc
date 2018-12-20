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
import com.meituan.dorado.rpc.handler.invoker.Invoker;
import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.rpc.meta.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 失败转移，当出现失败，重试其它节点，通常用于读操作，写建议重试为0或使用failfast
 *
 * @param <T>
 */
public class FailoverClusterHandler<T> extends ClusterHandler<T> {

    private static final Logger logger = LoggerFactory.getLogger(FailoverClusterHandler.class);
    private static final int MIN_RETRY_TIMES = 1;
    private static int retryTimes;

    public FailoverClusterHandler(InvokerRepository<T> repository) {
        super(repository);
        retryTimes = repository.getClientConfig().getFailoverRetryTimes();
        retryTimes = retryTimes < MIN_RETRY_TIMES ? MIN_RETRY_TIMES : retryTimes;
    }

    @Override
    protected RpcResult doInvoke(RpcInvocation invocation, List<Invoker<T>> invokers) throws Throwable {
        List<Invoker<T>> invoked = new ArrayList<>();
        Throwable recordExe = null;

        for (int i = 0; i <= retryTimes; i++) {
            Invoker<T> invoker = select(invocation, invokers, invoked);
            invoked.add(invoker);
            try {
                return invoker.invoke(invocation);
            } catch (Throwable e) {
                logger.warn("Failed " + i + " times, interface=" + invoker.getInterface().getName() +
                        "|method=" + invocation.getMethod().getName() + "|provider=" +
                        invoker.getProvider().getIp() + Constants.COLON + invoker.getProvider().getPort(), e);
                recordExe = e;
            }
        }
        if (recordExe != null) {
            throw recordExe;
        }
        return new RpcResult();
    }
}
