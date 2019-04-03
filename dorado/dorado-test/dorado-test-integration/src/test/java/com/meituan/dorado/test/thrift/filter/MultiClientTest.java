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

import com.meituan.dorado.test.thrift.apisuite.TestSuite;
import com.meituan.dorado.test.thrift.apitwitter.Twitter;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.ArrayList;
import java.util.List;

public class MultiClientTest {

    private static Logger logger = LoggerFactory.getLogger(MultiClientTest.class);

    private static ClassPathXmlApplicationContext clientBeanFactory;
    private static ClassPathXmlApplicationContext serverBeanFactory;
    private static Twitter.Iface client1;
    private static Twitter.Iface client2;
    private static TestSuite.Iface client3;

    private static StringBuilder client1ChainStr1 = new StringBuilder();
    private static StringBuilder client2ChainStr1 = new StringBuilder();
    private static StringBuilder client3ChainStr1 = new StringBuilder();

    @BeforeClass
    public static void start() {
        serverBeanFactory = new ClassPathXmlApplicationContext("thrift/filter/multiServer.xml");
        clientBeanFactory = new ClassPathXmlApplicationContext("thrift/filter/multiClient.xml");
        client1 = (Twitter.Iface) clientBeanFactory.getBean("clientProxy1");
        client2 = (Twitter.Iface) clientBeanFactory.getBean("clientProxy2");
        client3 = (TestSuite.Iface) clientBeanFactory.getBean("clientProxy3");
        ClientQpsLimitFilter.enable();
        ServerQpsLimitFilter.enable();
        ClientQpsLimitFilter.count.set(0);
        ServerQpsLimitFilter.count.set(0);
        buildExpectInvokeChainStr();
    }

    @Test
    public void testList() throws Exception {
        List<String> param = new ArrayList<String>();
        param.add("Test");
        FilterTest.invokeChainStr = new StringBuilder();
        List<String> result = client1.testList(param);
        Assert.assertEquals(param, result);
        Assert.assertEquals(client1ChainStr1.toString(), FilterTest.invokeChainStr.toString());

        FilterTest.invokeChainStr = new StringBuilder();
        result = client2.testList(param);
        Assert.assertEquals(param, result);
        Assert.assertEquals(client2ChainStr1.toString(), FilterTest.invokeChainStr.toString());

        FilterTest.invokeChainStr = new StringBuilder();
        String client3Ret = client3.testString("Test");
        Assert.assertEquals("Test", client3Ret);
        Assert.assertEquals(client3ChainStr1.toString(), FilterTest.invokeChainStr.toString());
    }

    @AfterClass
    public static void stop() {
        clientBeanFactory.destroy();
        serverBeanFactory.destroy();
        ClientQpsLimitFilter.disable();
        ServerQpsLimitFilter.disable();
    }

    private static void buildExpectInvokeChainStr() {
        client1ChainStr1.append(InvokerFilter.class.getSimpleName())
                .append(SpecificFilter.class.getSimpleName())
                .append(ClientQpsLimitFilter.class.getSimpleName())
                .append(CustomFilter2.class.getSimpleName())
                .append(ServerQpsLimitFilter.class.getSimpleName())
                .append(ProviderFilter.class.getSimpleName())
                .append(CustomFilter2.class.getSimpleName())
                .append(CustomFilter1.class.getSimpleName());
        client2ChainStr1.append(InvokerFilter.class.getSimpleName())
                .append(ClientQpsLimitFilter.class.getSimpleName())
                .append(CustomFilter1.class.getSimpleName())
                .append(ServerQpsLimitFilter.class.getSimpleName())
                .append(ProviderFilter.class.getSimpleName())
                .append(CustomFilter2.class.getSimpleName())
                .append(CustomFilter1.class.getSimpleName());
        client3ChainStr1.append(InvokerFilter.class.getSimpleName())
                .append(ClientQpsLimitFilter.class.getSimpleName())
                .append(CustomFilter2.class.getSimpleName())
                .append(CustomFilter1.class.getSimpleName())
                .append(SpecificFilter.class.getSimpleName())
                .append(ServerQpsLimitFilter.class.getSimpleName())
                .append(ProviderFilter.class.getSimpleName())
                .append(CustomFilter2.class.getSimpleName())
                .append(CustomFilter1.class.getSimpleName());
    }
}
