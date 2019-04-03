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

package com.meituan.dorado.test.thrift.normal;

import com.meituan.dorado.test.thrift.api.HelloService;
import com.meituan.dorado.test.thrift.filter.ClientQpsLimitFilter;
import com.meituan.dorado.test.thrift.filter.ServerQpsLimitFilter;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class HelloTest {
    private static ClassPathXmlApplicationContext clientBean;
    private static ClassPathXmlApplicationContext serverBean;

    @BeforeClass
    public static void init() {
        serverBean = new ClassPathXmlApplicationContext("thrift/normal/hello/thrift-provider.xml");
        clientBean = new ClassPathXmlApplicationContext("thrift/normal/hello/thrift-consumer.xml");
        ClientQpsLimitFilter.disable();
        ServerQpsLimitFilter.disable();
    }

    @Test
    public void test() {
        HelloService.Iface oriThriftClient = (HelloService.Iface) clientBean.getBean("oriThriftClient");
        HelloService.Iface octoProtocolClient = (HelloService.Iface) clientBean.getBean("octoProtocolClient");

        try {
            String ret1 = oriThriftClient.sayHello("Dorado");
            Assert.assertEquals("Hello Dorado", ret1);
            String ret2 = octoProtocolClient.sayBye("Dorado");
            Assert.assertEquals("Bye Dorado", ret2);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @AfterClass
    public static void stop() {
        clientBean.destroy();
        serverBean.destroy();
    }
}
