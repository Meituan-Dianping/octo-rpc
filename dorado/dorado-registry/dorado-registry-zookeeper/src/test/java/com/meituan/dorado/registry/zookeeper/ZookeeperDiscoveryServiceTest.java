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
package com.meituan.dorado.registry.zookeeper;

import com.meituan.dorado.common.extension.ExtensionLoader;
import com.meituan.dorado.registry.DiscoveryService;
import com.meituan.dorado.registry.RegistryFactory;
import com.meituan.dorado.registry.RegistryService;
import com.meituan.dorado.registry.meta.Provider;
import com.meituan.dorado.registry.meta.RegistryInfo;
import com.meituan.dorado.registry.meta.SubscribeInfo;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

public class ZookeeperDiscoveryServiceTest extends RegistryTest {

    private static RegistryService registryService;
    private static DiscoveryService discoveryService;

    @BeforeClass
    public static void init() throws Exception {
        initZkServer();
        RegistryFactory factory = ExtensionLoader.getExtensionWithName(RegistryFactory.class, "zookeeper");
        registryService = factory.getRegistryService(address);
        discoveryService = factory.getDiscoveryService(address);
    }

    @AfterClass
    public static void stop() throws Exception {
        discoveryService.destroy();
        registryService.destroy();
        zkServer.stop();
    }

    @Test
    public void testRegisterDiscovery() throws InterruptedException {
        RegistryInfo registryInfo = genRegistryInfo();
        SubscribeInfo subscribeInfo = genSubscribeInfo();
        MockNotifyListener listener = new MockNotifyListener();

        // 注册
        registryService.register(registryInfo);
        Thread.sleep(100);
        // 服务发现一个节点
        discoveryService.subscribe(subscribeInfo, listener);
        List<Provider> providers = listener.getProviders();
        Assert.assertEquals(1, providers.size());
        Assert.assertEquals(9001, providers.get(0).getPort());

        // 注销
        registryService.unregister(registryInfo);
        Thread.sleep(100);
        // 服务发现没有节点
        providers = listener.getProviders();
        Assert.assertEquals(0, providers.size());
        // 取消订阅
        discoveryService.unsubscribe(subscribeInfo);
    }

    @Test
    public void testUnsubscribe() throws InterruptedException {
        RegistryInfo registryInfo = genRegistryInfo();
        SubscribeInfo subscribeInfo = genSubscribeInfo();
        discoveryService.unsubscribe(subscribeInfo);
        MockNotifyListener listener = new MockNotifyListener();

        // 注册
        registryService.register(registryInfo);
        // 服务发现一个节点
        discoveryService.subscribe(subscribeInfo, listener);
        List<Provider> providers = listener.getProviders();
        Assert.assertEquals(1, providers.size());
        Assert.assertEquals(9001, providers.get(0).getPort());

        // 取消订阅
        discoveryService.unsubscribe(subscribeInfo);
        // 注销
        registryService.unregister(registryInfo);
        Thread.sleep(100);
        // 取消订阅后 没有发现服务变更
        providers = listener.getProviders();
        Assert.assertEquals(1, providers.size());
        Assert.assertEquals(9001, providers.get(0).getPort());
    }
}
