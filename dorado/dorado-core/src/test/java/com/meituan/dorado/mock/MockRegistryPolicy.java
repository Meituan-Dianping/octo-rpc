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

import com.meituan.dorado.registry.ProviderListener;
import com.meituan.dorado.registry.Registry;
import com.meituan.dorado.registry.RegistryPolicy;
import com.meituan.dorado.registry.meta.RegistryInfo;
import com.meituan.dorado.registry.meta.SubscribeInfo;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;


public class MockRegistryPolicy extends RegistryPolicy {

    public MockRegistryPolicy(Registry registry) {
        super(registry);
    }

    @Override
    protected void doRegister(RegistryInfo info) {

    }

    @Override
    protected void doUnregister(RegistryInfo info) {

    }

    @Override
    protected void doSubcribe(SubscribeInfo info, ProviderListener listener) {

    }

    @Override
    protected void doUnsubcribe(SubscribeInfo info) {

    }

    @Override
    public void destroy() {}

    public Set<RegistryInfo> getRegitryInfo() {
        return registered;
    }

    public ConcurrentMap<SubscribeInfo, ProviderListener> getSubsribeInfo() {
        return subscribed;
    }
}
