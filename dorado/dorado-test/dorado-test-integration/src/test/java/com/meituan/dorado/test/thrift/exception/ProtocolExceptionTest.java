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

import com.meituan.dorado.common.exception.TimeoutException;
import com.meituan.dorado.test.thrift.api2.TestService;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ProtocolExceptionTest {

    private static final Logger logger = LoggerFactory.getLogger(ProtocolExceptionTest.class);

    private static ClassPathXmlApplicationContext clientBeanFactory;
    private static ClassPathXmlApplicationContext serverBeanFactory;
    private static TestService.Iface client;


    @BeforeClass
    public static void start() throws InterruptedException {
        serverBeanFactory = new ClassPathXmlApplicationContext("thrift/exception/protocol/thrift-provider.xml");
        clientBeanFactory = new ClassPathXmlApplicationContext("thrift/exception/protocol/thrift-consumer.xml");
        client = (TestService.Iface) clientBeanFactory.getBean("clientProxy");
        Thread.sleep(5000);
    }

    @AfterClass
    public static void stop() {
        clientBeanFactory.destroy();
        serverBeanFactory.destroy();
    }

    // 服务端在业务逻辑中抛出ProtocolException,调用端显示TimeoutException
    @Test
    public void testProtocolMisMatch() {
        try {
            client.testProtocolMisMatch();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof TimeoutException);
        }
    }

    // IDL类文件中testLong_args的enum类的fieldId错误
    @Test
    public void testProtocolException() {
        try {
            client.testLong(1L);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
