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
package com.meituan.dorado.test.thrift.exception.server;

import com.meituan.dorado.test.thrift.apitwitter.Twitter;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;

public class ServerRejectExceptionTest {

    private static final Logger logger = LoggerFactory.getLogger(ServerRejectExceptionTest.class);

    private static ClassPathXmlApplicationContext clientBeanFactory;
    private static ClassPathXmlApplicationContext serverBeanFactory;
    private static Twitter.Iface client;

    @BeforeClass
    public static void start() throws InterruptedException {
        serverBeanFactory = new ClassPathXmlApplicationContext("thrift/exception/threadpool/thrift-provider.xml");
        clientBeanFactory = new ClassPathXmlApplicationContext("thrift/exception/threadpool/thrift-consumer.xml");
        client = (Twitter.Iface) clientBeanFactory.getBean("clientProxy");
        Thread.sleep(5000);
    }

    @AfterClass
    public static void stop() {
        clientBeanFactory.destroy();
        serverBeanFactory.destroy();
    }

    @Test
    public void testRejectException() throws InterruptedException {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(16);
        for(int i = 0; i < 16; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 4; i++) {
                        try {
                            String result = client.testString("string");
                            System.out.println("testString: " + result);
                        } catch (Exception e) {
                            Assert.assertTrue(e instanceof RejectedExecutionException);
                        }
                    }
                }
            });
        }
        Thread.sleep(100000);
    }
}
