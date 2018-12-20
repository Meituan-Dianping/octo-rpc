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

import com.meituan.dorado.registry.meta.Provider;
import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.rpc.meta.RpcResult;
import com.meituan.dorado.transport.Client;
import com.meituan.dorado.transport.meta.Request;

public interface Invoker<T> {

    Class<T> getInterface();

    RpcResult invoke(RpcInvocation invocation) throws Throwable;

    Provider getProvider();

    boolean updateProviderIfNeeded(Provider provider);

    Request genRequest();

    void destroy();

    boolean isDestroyed();

    Client getClient();
}
