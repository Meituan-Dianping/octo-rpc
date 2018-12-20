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

package com.meituan.dorado.registry.mns;

import com.meituan.dorado.common.extension.ExtensionLoader;
import com.meituan.dorado.common.util.NetUtil;
import com.meituan.dorado.common.util.VersionUtil;
import com.meituan.dorado.registry.DiscoveryService;
import com.meituan.dorado.registry.ProviderListener;
import com.meituan.dorado.registry.RegistryFactory;
import com.meituan.dorado.registry.RegistryService;
import com.meituan.dorado.registry.meta.Provider;
import com.meituan.dorado.registry.meta.RegistryInfo;
import com.meituan.dorado.registry.meta.SubscribeInfo;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.ArrayList;
import java.util.List;

public class MnsRegistryDiscoveryTest {
    private static final Logger logger = LoggerFactory.getLogger(MnsRegistryDiscoveryTest.class);

    private static ClassPathXmlApplicationContext beanFactory;
    private static RegistryService registryService;
    private static DiscoveryService discoveryService;

    @BeforeClass
    public static void init() {
        beanFactory = new ClassPathXmlApplicationContext("thrift-provider.xml");
        RegistryFactory factory = ExtensionLoader.getExtensionWithName(RegistryFactory.class, "mns");
        registryService = factory.getRegistryService("");
        discoveryService = factory.getDiscoveryService("");
    }

    @Test
    public void testRegisterDiscovery() throws InterruptedException {
        RegistryInfo registryInfo = genMnsRegistryInfo();
        SubscribeInfo subscribeInfo = genMnsSubscribeInfo();
        MockNotifyLinstener listener = new MockNotifyLinstener();

        // 注册
        registryService.register(registryInfo);
        Thread.sleep(10000);
        // 服务发现一个节点
        discoveryService.subcribe(subscribeInfo, listener);
        List<Provider> providers = listener.getProviders();
        Assert.assertEquals(1, providers.size());
        Assert.assertEquals(9001, providers.get(0).getPort());

        // 注销
        registryService.unregister(registryInfo);
        Thread.sleep(30000);
        // 服务发现没有节点
        providers = listener.getProviders();
        Assert.assertEquals(0, providers.size());
        // 取消订阅
        discoveryService.unsubcribe(subscribeInfo);
    }

    @Test
    public void testUnsubscribe() throws InterruptedException {
        RegistryInfo registryInfo = genMnsRegistryInfo();
        SubscribeInfo subscribeInfo = genMnsSubscribeInfo();
        discoveryService.unsubcribe(subscribeInfo);
        MockNotifyLinstener listener = new MockNotifyLinstener();

        // 注册
        registryService.register(registryInfo);
        Thread.sleep(15000);
        // 服务发现一个节点
        discoveryService.subcribe(subscribeInfo, listener);
        List<Provider> providers = listener.getProviders();
        Assert.assertEquals(1, providers.size());
        Assert.assertEquals(9001, providers.get(0).getPort());

        // 取消订阅
        discoveryService.unsubcribe(subscribeInfo);
        // 注销
        registryService.unregister(registryInfo);
        Thread.sleep(15000);
        // 取消订阅后 没有发现服务变更
        providers = listener.getProviders();
        Assert.assertEquals(1, providers.size());
        Assert.assertEquals(9001, providers.get(0).getPort());
    }

    public RegistryInfo genMnsRegistryInfo() {
        RegistryInfo registryInfo = new RegistryInfo();
        List<String> serviceNames = new ArrayList<>();
        serviceNames.add("com.meituan.mtthrift.test.HelloService");
        serviceNames.add("com.meituan.mtthrift.test.HelloService2");
        registryInfo.setServiceNames(serviceNames);
        registryInfo.setAppkey("com.meituan.octo.dorado.server");
        registryInfo.setIp(NetUtil.getLocalHost());
        registryInfo.setPort(9001);
        registryInfo.setProtocol("thrift");
        registryInfo.setVersion(VersionUtil.getDoradoVersion());
        registryInfo.setEnv("test");
        return registryInfo;
    }

    public SubscribeInfo genMnsSubscribeInfo() {
        SubscribeInfo mnsSubscribeInfo = new SubscribeInfo();
        mnsSubscribeInfo.setLocalAppkey("com.meituan.octo.dorado.client");
        mnsSubscribeInfo.setRemoteAppkey("com.meituan.octo.dorado.server");
        mnsSubscribeInfo.setServiceName("com.meituan.mtthrift.test.HelloService");
        mnsSubscribeInfo.setProtocol("thrift");

        return mnsSubscribeInfo;
    }

    @AfterClass
    public static void destroy() {
        beanFactory.destroy();
    }

    class MockNotifyLinstener implements ProviderListener {
        private List<Provider> providers;

        @Override
        public void notify(List<Provider> list) {
            providers = list;
            logger.info("Provider list changed: " + list);
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

        public List<Provider> getProviders() {
            return providers;
        }
    }
}
