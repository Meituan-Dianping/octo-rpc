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

package com.meituan.dorado.trace.cat;

import com.meituan.dorado.common.exception.TimeoutException;
import com.meituan.dorado.rpc.AsyncContext;
import com.meituan.dorado.rpc.ResponseFuture;
import com.meituan.dorado.trace.cat.api.Echo;
import org.apache.thrift.TException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class CatInvokeTraceTest {

    private static ClassPathXmlApplicationContext clientBeanFactory;
    private static ClassPathXmlApplicationContext serverBeanFactory;
    private static Echo.Iface client;
    private static Echo.Iface clientWithTimeout;
    private static final int loop = 100;

    @BeforeClass
    public static void start() {
        serverBeanFactory = new ClassPathXmlApplicationContext("thrift/thrift-provider.xml");
        clientBeanFactory = new ClassPathXmlApplicationContext("thrift/thrift-consumer.xml");
        client = (Echo.Iface) clientBeanFactory.getBean("clientProxy");
        clientWithTimeout = (Echo.Iface) clientBeanFactory.getBean("clientProxyWithTimeout");
    }

    @AfterClass
    public static void stop() {
        clientBeanFactory.destroy();
        serverBeanFactory.destroy();
    }

    @Test
    public void testCatTrace() {
        for (int i = 0; i < loop; i++) {
            try {
                String message = client.echo("this is a message");
                Assert.assertEquals("echo: this is a message", message);
            } catch (TException e) {
                Assert.fail(e.getMessage());
            }
        }
    }

    @Test
    public void testCatTraceWithTimeoutException() {
        for (int i = 0; i < loop; i++) {
            try {
                String message = clientWithTimeout.echo("this is a message");
                Assert.assertEquals("echo: this is a message", message);
            } catch (Exception e) {
                e.printStackTrace();
                Assert.assertTrue(e.getCause() instanceof TimeoutException);
            }
        }
    }

    @Test
    public void testAsyncCatTrace() throws ExecutionException, InterruptedException {
        for (int i = 0; i < loop; i++) {
            ResponseFuture<String> future = AsyncContext.getContext().asyncCall(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return client.echo("this is a message");
                }
            });
            Assert.assertEquals(future.get(), "echo: this is a message");
        }
        Thread.sleep(2000);
    }

}
