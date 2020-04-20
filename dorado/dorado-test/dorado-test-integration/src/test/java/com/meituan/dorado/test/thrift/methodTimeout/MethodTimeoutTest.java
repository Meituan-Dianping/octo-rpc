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
package com.meituan.dorado.test.thrift.methodTimeout;


import com.meituan.dorado.common.exception.TimeoutException;
import com.meituan.dorado.rpc.AsyncContext;
import com.meituan.dorado.rpc.ResponseFuture;
import com.meituan.dorado.test.thrift.api.HelloService;
import com.meituan.dorado.test.thrift.filter.ClientQpsLimitFilter;
import com.meituan.dorado.test.thrift.filter.ServerQpsLimitFilter;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.sql.Time;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class MethodTimeoutTest {

    private static ClassPathXmlApplicationContext clientBeanFactory;
    private static ClassPathXmlApplicationContext serverBeanFactory;
    private static HelloService.Iface client;

    @BeforeClass
    public static void start() throws InterruptedException {
        serverBeanFactory = new ClassPathXmlApplicationContext("thrift/methodTimeout/thrift-provider.xml");
        clientBeanFactory = new ClassPathXmlApplicationContext("thrift/methodTimeout/thrift-consumer.xml");
        client = (HelloService.Iface) clientBeanFactory.getBean("clientProxy");
        ClientQpsLimitFilter.disable();
        ServerQpsLimitFilter.disable();
    }

    @AfterClass
    public static void stop() {
        clientBeanFactory.destroy();
        serverBeanFactory.destroy();
    }

    @Test
    public void testMethodTimeout() {
        try {
            client.sayHello("this is a message");
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof TimeoutException);
        }
    }

    @Test
    public void testAsync() {
        ResponseFuture<String> future = AsyncContext.getContext().asyncCall(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return client.sayHello("Dorado async");
            }
        });
        try {
            future.get();
        } catch (ExecutionException e) {
            Assert.assertTrue(e.getCause() instanceof TimeoutException);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

    }
}
