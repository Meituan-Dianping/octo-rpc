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

import com.meituan.dorado.bootstrap.ServiceBootstrap;
import com.meituan.dorado.config.service.ReferenceConfig;
import com.meituan.dorado.test.thrift.api.HelloService;
import org.apache.thrift.TException;
import org.junit.Assert;

public class ThriftConsumer {

    private static HelloService.Iface client;

    public static void main(String[] args) {
        ReferenceConfig<HelloService.Iface> config = new ReferenceConfig<>();
        config.setAppkey("com.meituan.octo.dorado.client");
        config.setRemoteAppkey("com.meituan.octo.dorado.server");
        config.setServiceInterface(HelloService.class);
        config.setRegistry("zookeeper://127.0.0.1:2200");

        try {
            client = config.get();
            String result = client.sayHello("meituan");
            System.out.println(result);
            Assert.assertEquals(result, "Hello meituan");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            config.destroy();
        }

        ServiceBootstrap.clearGlobalResource();
        System.exit(0);
    }
}
