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
package com.meituan.dorado.registry.mock;

import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.util.NetUtil;
import com.meituan.dorado.registry.DiscoveryService;
import com.meituan.dorado.registry.ProviderListener;
import com.meituan.dorado.registry.RegistryPolicy;
import com.meituan.dorado.registry.meta.Provider;
import com.meituan.dorado.registry.meta.SubscribeInfo;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class MockDiscoveryService implements DiscoveryService {

    private final String NAME = "mock";

    public MockDiscoveryService(String address) {
    }

    @Override
    public void subscribe(SubscribeInfo info, ProviderListener listener) {
        List<Provider> providerList = new ArrayList<>();
        Provider provider = new Provider();
        provider.setIp(NetUtil.getLocalHost());
        provider.setPort(Constants.DEFAULT_SERVER_PORT);
        listener.notify(providerList);
    }

    @Override
    public void unsubscribe(SubscribeInfo info) {
        // just mock do nothing
    }

    @Override
    public void destroy() {
        // just mock do nothing
    }

    @Override
    public void setRegistryPolicy(RegistryPolicy registryPolicy) {
        // just mock do nothing
    }
}
