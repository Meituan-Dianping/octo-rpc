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
package com.meituan.dorado.test.thrift.exception.client;

import com.meituan.dorado.test.thrift.apitwitter.Twitter;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class EmptyChannelTest {

    private static final Logger logger = LoggerFactory.getLogger(EmptyChannelTest.class);

    private static ClassPathXmlApplicationContext clientBeanFactory;
    private static ClassPathXmlApplicationContext serverBeanFactory;
    private static Twitter.Iface client;

    @BeforeClass
    public static void start() throws InterruptedException {
        serverBeanFactory = new ClassPathXmlApplicationContext("thrift/exception/emptyChannel/thrift-provider.xml");
        clientBeanFactory = new ClassPathXmlApplicationContext("thrift/exception/emptyChannel/thrift-consumer.xml");
        client = (Twitter.Iface) clientBeanFactory.getBean("clientProxy");
        Thread.sleep(5000);
    }

    @AfterClass
    public static void stop() {
        clientBeanFactory.destroy();
        serverBeanFactory.destroy();
    }

    // 测试需要修改代码返回的channel为null
    @Test
    @Ignore
    public void testEmptyChannel() {
        try {
            client.testString("test string.");
        } catch (Exception e) {
            Assert.fail();
        }
    }

}
