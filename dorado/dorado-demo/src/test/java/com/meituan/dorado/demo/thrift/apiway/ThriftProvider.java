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
package com.meituan.dorado.demo.thrift.apiway;

import com.meituan.dorado.config.service.ProviderConfig;
import com.meituan.dorado.config.service.ServiceConfig;
import com.meituan.dorado.test.thrift.api.HelloService;
import com.meituan.dorado.test.thrift.api.HelloServiceImpl;
import org.apache.curator.test.TestingServer;

public class ThriftProvider {

    protected static TestingServer zkServer;

    public static void initZkServer() throws Exception {
        int zkServerPort = 2200;
        zkServer = new TestingServer(zkServerPort, true);
    }

    public static void main(String[] args) throws Exception {
        initZkServer();

        ServiceConfig<HelloService.Iface> serviceConfig = new ServiceConfig<>();
        serviceConfig.setServiceImpl(new HelloServiceImpl());
        serviceConfig.setServiceInterface(HelloService.class);
        serviceConfig.setServiceName(HelloService.class.getName());

        ProviderConfig config = new ProviderConfig();
        config.setAppkey("com.meituan.octo.dorado.server");
        config.setServiceConfig(serviceConfig);
        // 这里是模拟zkserver
        config.setRegistry("zookeeper://127.0.0.1:2200");
        config.setPort(9001);
        config.init();

        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            config.destroy();
        }
        zkServer.close();
    }
}
