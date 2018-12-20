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
package com.meituan.dorado.mock;


import com.meituan.dorado.config.service.ClientConfig;
import com.meituan.dorado.registry.meta.Provider;
import com.meituan.dorado.rpc.handler.filter.FilterHandler;
import com.meituan.dorado.rpc.handler.invoker.AbstractInvoker;
import com.meituan.dorado.transport.meta.Request;

public class MockInvoker extends AbstractInvoker {

    public MockInvoker(Provider provider, Class<?> iface, ClientConfig config) {
        super(config, provider);
        this.provider = provider;
        this.serviceInterface = iface;
    }

    public MockInvoker(Provider provider, Class<?> iface) {
        this.provider = provider;
        this.serviceInterface = iface;
    }

    @Override
    public Request genRequest() {
        return new MockRequest();
    }

}
