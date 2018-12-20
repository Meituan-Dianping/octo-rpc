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

import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.util.NetUtil;
import com.meituan.dorado.common.util.VersionUtil;
import com.meituan.dorado.registry.ProviderListener;
import com.meituan.dorado.registry.meta.Provider;
import com.meituan.dorado.bootstrap.provider.ProviderStatus;
import com.meituan.dorado.registry.meta.RegistryInfo;
import com.meituan.dorado.registry.meta.SubscribeInfo;
import org.apache.curator.test.TestingServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RegistryTest {

    private static final Logger logger = LoggerFactory.getLogger(RegistryTest.class);

    protected static TestingServer zkServer;
    protected static String address;

    public static void initZkServer() throws Exception {
        int zkServerPort = NetUtil.getAvailablePort(2000);
        zkServer = new TestingServer(zkServerPort, true);
        address = NetUtil.getLocalHost() + Constants.COLON + zkServerPort;
    }

    public class MockNotifyListener implements ProviderListener {
        private ConcurrentMap<String, Provider> providers = new ConcurrentHashMap<>();
        ;

        @Override
        public void notify(List<Provider> list) {
            if (list == null || list.isEmpty()) {
                return;
            }
            for (Provider provider : list) {
                providers.put(provider.getIp() + Constants.COLON + provider.getPort(), provider);
            }
            logger.info("Provider lists: {}", list);
        }

        @Override
        public void added(List<Provider> newList) {
            if (newList == null || newList.isEmpty()) {
                return;
            }
            for (Provider provider : newList) {
                providers.put(provider.getIp() + Constants.COLON + provider.getPort(), provider);
                logger.info("Provider list add: " + newList);
            }
        }

        @Override
        public void updated(List<Provider> updated) {
            for (Provider provider : updated) {
                Provider old = providers.get(provider);
                if (old != null) {
                    old.updateIfDiff(provider);
                    logger.info("Provider list update: {}", provider);
                }
            }
        }

        @Override
        public void removed(List<String> ipPorts) {
            for (String ipPort : ipPorts) {
                Provider removed = providers.remove(ipPort);
                logger.info("Provider list removed: {}", removed);
            }
        }

        public List<Provider> getProviders() {
            return new ArrayList<Provider>(providers.values());
        }
    }

    public RegistryInfo genRegistryInfo() {
        RegistryInfo registryInfo = new RegistryInfo();
        List<String> serviceNames = new ArrayList<>();
        serviceNames.add("com.meituan.mtthrift.test.HelloService");
        serviceNames.add("com.meituan.mtthrift.test.HelloService2");
        registryInfo.setServiceNames(serviceNames);
        registryInfo.setAppkey("com.meituan.octo.dorado.server");
        registryInfo.setIp(NetUtil.getLocalHost());
        registryInfo.setPort(9001);
        registryInfo.setProtocol("thrift");
        registryInfo.setWeight(10);
        registryInfo.setVersion(VersionUtil.getDoradoVersion());
        registryInfo.setEnv("test");
        registryInfo.setStatus(ProviderStatus.ALIVE.getCode());
        return registryInfo;
    }

    public SubscribeInfo genSubscribeInfo() {
        SubscribeInfo mnsSubscribeInfo = new SubscribeInfo();
        mnsSubscribeInfo.setLocalAppkey("com.meituan.octo.dorado.client");
        mnsSubscribeInfo.setRemoteAppkey("com.meituan.octo.dorado.server");
        mnsSubscribeInfo.setServiceName("com.meituan.mtthrift.test.HelloService");
        mnsSubscribeInfo.setProtocol("thrift");
        mnsSubscribeInfo.setEnv("test");

        return mnsSubscribeInfo;
    }
}
