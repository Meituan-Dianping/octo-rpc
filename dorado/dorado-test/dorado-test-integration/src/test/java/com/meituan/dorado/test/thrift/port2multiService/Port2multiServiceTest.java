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
package com.meituan.dorado.test.thrift.port2multiService;

import com.meituan.dorado.common.exception.TimeoutException;
import com.meituan.dorado.test.thrift.api.Echo;
import com.meituan.dorado.test.thrift.api.HelloService;
import com.meituan.dorado.test.thrift.filter.ClientQpsLimitFilter;
import com.meituan.dorado.test.thrift.filter.ServerQpsLimitFilter;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Port2multiServiceTest {

    private static ClassPathXmlApplicationContext clientBeanFactory;
    private static ClassPathXmlApplicationContext serverBeanFactory;
    private static Echo.Iface client1;
    private static HelloService.Iface client2;


    @BeforeClass
    public static void start() {
        serverBeanFactory = new ClassPathXmlApplicationContext("thrift/port2multiService/thrift-provider.xml");
        clientBeanFactory = new ClassPathXmlApplicationContext("thrift/port2multiService/thrift-consumer.xml");
        client1 = (Echo.Iface) clientBeanFactory.getBean("clientProxy1");
        client2 = (HelloService.Iface) clientBeanFactory.getBean("clientProxy2");

        ClientQpsLimitFilter.disable();
        ServerQpsLimitFilter.disable();
    }

    @AfterClass
    public static void stop() {
        clientBeanFactory.destroy();
        serverBeanFactory.destroy();
    }

    @Test
    public void testMultiServiceOctoProtocol() {
        try {
            String message = client1.echo("this is a message");
            Assert.assertEquals("echo: this is a message", message);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testMultiServiceOriProtocol() {
        try {
            String message = client2.sayHello("this is a message");
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof TimeoutException);
        }
    }

}
