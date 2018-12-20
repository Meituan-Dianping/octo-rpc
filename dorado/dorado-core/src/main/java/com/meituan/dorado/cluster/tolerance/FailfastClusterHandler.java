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
import com.meituan.dorado.rpc.handler.invoker.Invoker;
import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.rpc.meta.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 快速失败，只发起一次调用，失败立即报错，通常用于非幂等性的写操作，即重复执行会出现不同结果的操作。
 */
public class FailfastClusterHandler<T> extends ClusterHandler<T> {

    public FailfastClusterHandler(InvokerRepository repository) {
        super(repository);
    }

    @Override
    protected RpcResult doInvoke(RpcInvocation invocation, List<Invoker<T>> invokers) throws Throwable {
        Invoker<T> invoker = select(invocation, invokers, new ArrayList<Invoker<T>>());
        return invoker.invoke(invocation);
    }
}
