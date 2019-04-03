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
package com.meituan.dorado.test.thrift.filter;

import com.meituan.dorado.common.exception.FilterException;
import com.meituan.dorado.common.exception.RemoteException;
import com.meituan.dorado.rpc.handler.filter.Filter;
import com.meituan.dorado.test.thrift.apitwitter.Tweet;
import com.meituan.dorado.test.thrift.apitwitter.Twitter;
import com.meituan.dorado.test.thrift.apitwitter.TwitterUnavailable;
import org.apache.thrift.TException;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FilterTest {

    private static Logger logger = LoggerFactory.getLogger(FilterTest.class);

    private static ClassPathXmlApplicationContext clientBeanFactory;
    private static ClassPathXmlApplicationContext serverBeanFactory;
    private static Twitter.Iface client;

    public static StringBuilder invokeChainStr = new StringBuilder();
    public static StringBuilder exceptionInfoStr = new StringBuilder();

    private static StringBuilder expectClientInvokeChainStr = new StringBuilder();
    private static StringBuilder expectAllInvokeChainStr = new StringBuilder();
    private static StringBuilder expectExceptionInfoStr = new StringBuilder();

    private String testStr = "I am Emma";

    @BeforeClass
    public static void start() {
        serverBeanFactory = new ClassPathXmlApplicationContext("thrift/filter/server.xml");
        clientBeanFactory = new ClassPathXmlApplicationContext("thrift/filter/client.xml");
        client = (Twitter.Iface) clientBeanFactory.getBean("clientProxy");
        ClientQpsLimitFilter.enable();
        ServerQpsLimitFilter.enable();
        List<Filter> filters = new ArrayList<Filter>();
        filters.add(new TraceFilter());
        buildExpectInvokeChainStr();
    }

    @Before
    public void beforeTest() {
        ClientQpsLimitFilter.count.set(0);
        ServerQpsLimitFilter.count.set(0);
    }

    @Test
    public void testClientInvoke() throws TException {
        invokeChainStr = new StringBuilder();
        List<String> param = new ArrayList<String>();
        List<String> result = client.testList(param);
        Assert.assertEquals(param, result);
        Assert.assertEquals(expectAllInvokeChainStr.toString(), invokeChainStr.toString());
    }

    @Test
    public void testClientFilterException() throws TException {
        ClientQpsLimitFilter.enable();
        ServerQpsLimitFilter.disable();
        Map<String, String> param = new HashMap<String, String>();
        for (int i = 0; i < 3; i++) {
            Map<String, String> result = client.testMap(param);
            Assert.assertEquals(param, result);
        }

        try {
            String result = client.testString(testStr);
        } catch (Exception e) {
            Assert.assertEquals(FilterException.class, e.getClass());
            Assert.assertEquals("QpsLimited", e.getMessage());
        }
    }

    @Test
    public void testServerFilterException() throws TException {
        ClientQpsLimitFilter.disable();
        ServerQpsLimitFilter.enable();
        byte param = 'a';
        for (int i = 0; i < 5; i++) {
            byte result = client.testByte(param);
            Assert.assertEquals(param, result);
        }

        try {
            String result = client.testString(testStr);
            Assert.fail();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertEquals(RemoteException.class, e.getClass());
            String exceptionMessage = "QpsLimited";
            Assert.assertEquals(true, e.getCause().getMessage().contains(exceptionMessage));
        }
    }

    @Test
    public void testException() throws NoSuchMethodException, TException {
        exceptionInfoStr = new StringBuilder();
        try {
            String result = client.testException(new Tweet(1, "a", "b"));
        } catch (TwitterUnavailable twitterUnavailable) {
            Assert.assertEquals(expectExceptionInfoStr.toString(), exceptionInfoStr.toString());
        }
    }

    @AfterClass
    public static void stop() throws InterruptedException {
        clientBeanFactory.destroy();
        serverBeanFactory.destroy();
    }

    private static void buildExpectInvokeChainStr() {
        expectClientInvokeChainStr.append(TraceFilter.class.getSimpleName())
                .append(InvokerFilter.class.getSimpleName())
                .append(ClientQpsLimitFilter.class.getSimpleName());

        expectAllInvokeChainStr.append(TraceFilter.class.getSimpleName())
                .append(InvokerFilter.class.getSimpleName())
                .append(ClientQpsLimitFilter.class.getSimpleName())
                .append(CustomFilter2.class.getSimpleName())
                .append(CustomFilter1.class.getSimpleName())
                .append(TraceFilter.class.getSimpleName())
                .append(ServerQpsLimitFilter.class.getSimpleName())
                .append(ProviderFilter.class.getSimpleName())
                .append(CustomFilter2.class.getSimpleName())
                .append(CustomFilter1.class.getSimpleName());

        expectExceptionInfoStr.append("ProviderFilter end have Exception").append("InvokerFilter end have Exception");
    }
}
