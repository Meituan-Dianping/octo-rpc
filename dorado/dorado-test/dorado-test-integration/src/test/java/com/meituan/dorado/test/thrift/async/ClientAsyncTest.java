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

package com.meituan.dorado.test.thrift.async;

import com.meituan.dorado.bootstrap.ServiceBootstrap;
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

public class ClientAsyncTest {

    private static ClassPathXmlApplicationContext clientBeanFactory;
    private static ClassPathXmlApplicationContext serverBeanFactory;
    private static HelloService.Iface client1;

    @BeforeClass
    public static void start() {
        serverBeanFactory = new ClassPathXmlApplicationContext("thrift/async/thrift-provider-async.xml");
        clientBeanFactory = new ClassPathXmlApplicationContext("thrift/async/thrift-consumer-async.xml");
        client1 = (HelloService.Iface) clientBeanFactory.getBean("helloService");
        ClientQpsLimitFilter.disable();
        ServerQpsLimitFilter.disable();
    }

    @AfterClass
    public static void stop() {
        clientBeanFactory.destroy();
        serverBeanFactory.destroy();
        ServiceBootstrap.clearGlobalResource();
    }

    @Test
    public void asyncTest() {

        try {
            //1.  同步调用
            long start = System.currentTimeMillis();
            System.out.println(client1.sayHello("Emma"));
            long end = System.currentTimeMillis();
            System.out.println("同步调用 cost:" + (end - start) + "ms");

            System.out.println();
            //2. 异步调用
            start = System.currentTimeMillis();
            ResponseFuture<String> future = AsyncContext.getContext().asyncCall(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return client1.sayHello("Emma async");
                }
            });
            end = System.currentTimeMillis();
            System.out.println("异步调用发起 cost:" + (end - start) + "ms");

            start = System.currentTimeMillis();
            System.out.println(future.get());
            end = System.currentTimeMillis();
            System.out.println("异步调用获取结果 cost:" + (end - start) + "ms");

            System.out.println();
            //3. 异步回调
            start = System.currentTimeMillis();
            ResponseFuture<String> future2 = AsyncContext.getContext().asyncCall(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return client1.sayHello("Emma async callback");
                }
            });
            end = System.currentTimeMillis();
            System.out.println("异步调用发起 cost:" + (end - start) + "ms");

            start = System.currentTimeMillis();
            future2.setCallback(new ResponseCallback<String>() {
                @Override
                public void onComplete(String result) {
                    System.out.println(result);
                }

                @Override
                public void onError(Throwable e) {
                    System.out.println("回调获得异常" + e.getMessage());
                }
            });
            end = System.currentTimeMillis();
            System.out.println("异步调用设置回调 cost:" + (end - start) + "ms");
            Thread.sleep(5000);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}
