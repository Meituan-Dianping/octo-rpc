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
package com.meituan.dorado.test.thrift.timeout;

import com.meituan.dorado.common.exception.TimeoutException;
import com.meituan.dorado.rpc.AsyncContext;
import com.meituan.dorado.rpc.ResponseCallback;
import com.meituan.dorado.rpc.ResponseFuture;
import com.meituan.dorado.test.thrift.api.HelloService;
import com.meituan.dorado.test.thrift.filter.ClientQpsLimitFilter;
import com.meituan.dorado.test.thrift.filter.ServerQpsLimitFilter;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.Callable;

public class TimeoutTest {

    private static ClassPathXmlApplicationContext clientBeanFactory;
    private static ClassPathXmlApplicationContext serverBeanFactory;
    private static HelloService.Iface client;

    @BeforeClass
    public static void start() throws InterruptedException {
        serverBeanFactory = new ClassPathXmlApplicationContext("thrift/timeout/thrift-provider.xml");
        clientBeanFactory = new ClassPathXmlApplicationContext("thrift/timeout/thrift-consumer.xml");
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
    public void testSyncTimeout() {
        try {
            client.sayHello("wxy");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertTrue(e instanceof TimeoutException);
        }
    }

    @Test
    public void testAsyncTimeout() {
        ResponseFuture<String> future = AsyncContext.getContext().asyncCall(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return client.sayHello("wxy-async");
            }
        });
        try {
            String response = future.get();
            assert "Hello wxy-async".equals(response);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof TimeoutException);
        }
    }

    @Test
    public void testAsyncTimeoutWithCallback() {
        ResponseFuture<String> future = AsyncContext.getContext().asyncCall(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return client.sayHello("wxy-async-callback");
            }
        });
        future.setCallback(new ResponseCallback<String>() {
            @Override
            public void onComplete(String result) {
                assert "Hello wxy-async-callback".equals(result);
            }

            @Override
            public void onError(Throwable e) {
                Assert.assertTrue(e instanceof TimeoutException);
            }
        });

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
    }

}
