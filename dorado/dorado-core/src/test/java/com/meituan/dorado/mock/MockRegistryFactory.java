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

import com.meituan.dorado.registry.DiscoveryService;
import com.meituan.dorado.registry.ProviderListener;
import com.meituan.dorado.registry.RegistryPolicy;
import com.meituan.dorado.registry.RegistryService;
import com.meituan.dorado.registry.meta.RegistryInfo;
import com.meituan.dorado.registry.meta.SubscribeInfo;
import com.meituan.dorado.registry.support.AbstractRegistryFactory;

public class MockRegistryFactory extends AbstractRegistryFactory {

    private final String NAME = "mock";

    @Override
    public RegistryService getRegistryService(String address) {
        return new MockRegistryService(address);
    }

    @Override
    public DiscoveryService getDiscoveryService(String address) {
        return new MockDiscoveryService(address);
    }

    @Override
    public String getName() {
        return NAME;
    }

    public static class MockRegistryService implements RegistryService {

        public MockRegistryService(String address) {
        }

        @Override
        public void register(RegistryInfo info) {
            // just mock do nothing
        }

        @Override
        public void unregister(RegistryInfo info) {
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

    public static class MockDiscoveryService implements DiscoveryService {

        private final String NAME = "mock";

        public MockDiscoveryService(String address) {
        }

        @Override
        public void subscribe(SubscribeInfo info, ProviderListener listener) {

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

}

