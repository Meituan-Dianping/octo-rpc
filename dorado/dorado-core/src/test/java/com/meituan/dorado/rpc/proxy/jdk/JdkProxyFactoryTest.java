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
package com.meituan.dorado.rpc.proxy.jdk;


import com.meituan.dorado.cluster.ClusterHandler;
import com.meituan.dorado.cluster.InvokerRepository;
import com.meituan.dorado.config.service.ClientConfig;
import com.meituan.dorado.config.service.ReferenceConfig;
import com.meituan.dorado.mock.MockCluster;
import org.junit.Assert;
import org.junit.Test;

public class JdkProxyFactoryTest {

    @Test
    public void testGetProxy() {
        JdkProxyFactory factory = new JdkProxyFactory();

        ReferenceConfig config = new ReferenceConfig();
        config.setRegistry("mock://1.2.3.4:1000");
        config.setAppkey("com.meituan.octo.dorado.client");
        config.setRemoteAppkey("com.meituan.octo.dorado.server");
        config.setProtocol("mock");
        config.setServiceInterface(HelloService.class);

        ClientConfig clientConfig = new ClientConfig(config);
        ClusterHandler<HelloService> handler = new MockCluster.MockClusterHandler(new InvokerRepository(clientConfig));
        Object service = factory.getProxy(handler);
        Assert.assertTrue(service instanceof HelloService);
    }

    interface HelloService {
        String sayHello(String message);
    }
}
