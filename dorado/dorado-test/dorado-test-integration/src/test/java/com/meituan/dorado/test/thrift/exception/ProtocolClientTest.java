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
package com.meituan.dorado.test.thrift.exception;

import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 测试调用端IDL类和服务端不一致的情况
 */
@Ignore
public class ProtocolClientTest {

    private static final Logger logger = LoggerFactory.getLogger(ProtocolClientTest.class);

    private static ClassPathXmlApplicationContext clientBeanFactory;
    private static com.meituan.dorado.test.thrift.exception.api1.ApiVersion1.Iface client;
    private static com.meituan.dorado.test.thrift.exception.api2.ApiVersion1.Iface client2;


    @BeforeClass
    public static void start() {
        clientBeanFactory = new ClassPathXmlApplicationContext("thrift/exception/protocolClient/thrift-consumer.xml");
        client = (com.meituan.dorado.test.thrift.exception.api1.ApiVersion1.Iface) clientBeanFactory.getBean("clientProxy");
        client2 = (com.meituan.dorado.test.thrift.exception.api2.ApiVersion1.Iface) clientBeanFactory.getBean("clientProxy2");
    }

    @AfterClass
    public static void stop() {
        clientBeanFactory.destroy();
    }

    @Test
    public void testClient() {
        try {
            com.meituan.dorado.test.thrift.exception.api1.Result result = client.send("message", "param");
            logger.info("EchoResult: " + result);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testClient2() {
        try {
            com.meituan.dorado.test.thrift.exception.api2.Result result = client2.send("message", "param");
            logger.info("EchoResult2: " + result);
        } catch(Exception e) {
            Assert.fail();
        }
    }
}
