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
package com.meituan.dorado.registry.support;

import com.meituan.dorado.mock.MockRegistryFactory;
import com.meituan.dorado.registry.ProviderListener;
import com.meituan.dorado.registry.meta.Provider;
import com.meituan.dorado.registry.meta.RegistryInfo;
import com.meituan.dorado.registry.meta.SubscribeInfo;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class FailbackRegistryTest {

    @Test
    public void testDoRegisterAndUnregister() {
        RegistryInfo info = new RegistryInfo();
        List<String> serviceNameList = Arrays.asList("testA", "testB");
        info.setServiceNames(serviceNameList);

        FailbackRegistry registry = new FailbackRegistry(new MockRegistryFactory.MockRegistryService(""));
        registry.doRegister(info);
        registry.doUnregister(info);
    }

    @Test
    public void testDoSubscribeAndUnsubscribe() {
        SubscribeInfo info = new SubscribeInfo();
        info.setLocalAppkey("com.meituan.octo.dorado.client");
        info.setRemoteAppkey("com.meituan.octo.dorado.server");

        FailbackRegistry registry = new FailbackRegistry(new MockRegistryFactory.MockDiscoveryService(""));
        registry.doSubcribe(info, new ProviderListener() {
            @Override
            public void notify(List<Provider> providers) {
                for (Provider provider : providers) {
                    provider.notify();
                }
            }

            @Override
            public void added(List<Provider> providers) {
            }

            @Override
            public void updated(List<Provider> providers) {
            }

            @Override
            public void removed(List<String> ipPorts) {
            }
        });
        registry.doUnsubcribe(info);
    }

}
