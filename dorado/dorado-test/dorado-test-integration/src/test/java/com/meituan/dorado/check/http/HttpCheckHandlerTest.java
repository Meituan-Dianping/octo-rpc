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
package com.meituan.dorado.check.http;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.meituan.dorado.bootstrap.ServiceBootstrap;
import com.meituan.dorado.common.extension.ExtensionLoader;
import com.meituan.dorado.transport.http.HttpServer;
import org.junit.*;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ConnectException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class HttpCheckHandlerTest {

    private final String[] methods = new String[]{
            "testMessage(com.meituan.dorado.test.thrift.apisuite.Message):Message",
            "testBaseTypeReturnException():int",
            "testMockProtocolException():void",
            "testSubMessage(com.meituan.dorado.test.thrift.apisuite.SubMessage):SubMessage",
            "testMessageList(java.util.List):List",
            "testReturnNull():String",
            "testIDLException():String",
            "testNPE():String",
            "testMultiIDLException():String",
            "testTimeout():void",
            "testVoid():void",
            "testString(java.lang.String):String",
            "testMessageMap(java.util.Map):Map",
            "testStrMessage(java.lang.String, com.meituan.dorado.test.thrift.apisuite.SubMessage):SubMessage",
            "testLong(long):long",
            "multiParam(byte, int, long, double):Map"};
    private int port;
    private ClassPathXmlApplicationContext serverBeanFactory = null;
    private ClassPathXmlApplicationContext clientBeanFactory = null;

    @After
    public void tearDown() throws Exception {
        HttpServer httpServer = ServiceBootstrap.getHttpServer();
        if (httpServer != null) {
            httpServer.close();
            Field field = ServiceBootstrap.class.getDeclaredField("httpServer");
            field.setAccessible(true);
            field.set(ServiceBootstrap.class, null);
        }

        {
            Field field = ExtensionLoader.class.getDeclaredField("extensionMap");
            Field field1 = ExtensionLoader.class.getDeclaredField("extensionListMap");
            field.setAccessible(true);
            field1.setAccessible(true);
            ((Map)field.get(ExtensionLoader.class)).clear();
            ((Map)field1.get(ExtensionLoader.class)).clear();
        }

        if (serverBeanFactory != null) {
            serverBeanFactory.destroy();
            serverBeanFactory = null;
        }
        if (clientBeanFactory != null) {
            clientBeanFactory.destroy();
            clientBeanFactory = null;
        }
    }

    /**
     * 测试获取服务端基本信息
     */
    @Test
    public void onlySupportProviderHttpCheckHandlerWhenStartDoradoProvider() {
         serverBeanFactory = new ClassPathXmlApplicationContext("thrift/httpCheck/thrift-provider.xml");
        port = ServiceBootstrap.getHttpServer().getLocalAddress().getPort();
        assertSuccessProviderHttpCheckHandler();
        assertFailureConsumerHttpCheckHandler();


    }

    @Test
    public void onlySupportAllHttpCheckHandlerWhenStartDoradoProviderAndConsumer() {
         serverBeanFactory = new ClassPathXmlApplicationContext("thrift/httpCheck/thrift-provider.xml");
         clientBeanFactory = new ClassPathXmlApplicationContext("thrift/httpCheck/thrift-consumer.xml");


        port = ServiceBootstrap.getHttpServer().getLocalAddress().getPort();
        assertSuccessProviderHttpCheckHandler();
        assertSuccessConsumerHttpCheckHandler();

    }

    @Test
    public void onlySupportConsumerHttpCheckHandlerWhenStartDoradoConsumer() {
        try {
            clientBeanFactory = new ClassPathXmlApplicationContext("thrift/httpCheck/thrift-consumer.xml");
        } catch (Exception e) {
            if (!(e.getCause().getCause().getCause() instanceof ConnectException)) {
                throw e;
            }
        }


        port = ServiceBootstrap.getHttpServer().getLocalAddress().getPort();
        assertFailureProviderHttpCheckHandler();
        assertSuccessConsumerHttpCheckHandler();
    }

    private void assertFailureProviderHttpCheckHandler() {
        // 1. Assert the provider service.info path
        {
            String url = String.format("http://localhost:%d/service.info", port);
            String result = HttpClientUtil.doGet(url, null);
            Assert.assertEquals("{\"success\":false,\"result\":\"INVOKER not support the request. Support uri: []\"}", result);
        }

        // 2. Assert the provider method.info path
        {
            String url = String.format("http://localhost:%d/method.info", port);
            String result = HttpClientUtil.doGet(url, null);
            Assert.assertEquals("{\"success\":false,\"result\":\"INVOKER not support the request. Support uri: []\"}", result);
        }

    }

    private void assertSuccessProviderHttpCheckHandler() {
        // 1. Assert the provider service.info path
        {
            String url = String.format("http://localhost:%d/service.info", port);
            String result = HttpClientUtil.doGet(url, null);

            JsonNode providerInfo = null;
            try {
                providerInfo = new ObjectMapper().readTree(result);
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
            Assert.assertEquals(4, providerInfo.size());
            Assert.assertEquals("com.meituan.octo.dorado.server", providerInfo.get("appkey").asText());
            Assert.assertEquals(2, providerInfo.get("serviceInfo").size());
            Assert.assertEquals("9001", providerInfo.get("serviceInfo").get(0).get("port").asText());

            Assert.assertEquals(1, providerInfo.get("serviceInfo").get(0).get("serviceIfaceInfos").size());
            Assert.assertEquals("com.meituan.dorado.test.thrift.apisuite.TestSuite", providerInfo.get("serviceInfo").get(0).get("serviceIfaceInfos").get(0).get("serviceName").asText());
            Assert.assertEquals("com.meituan.dorado.test.thrift.apisuite.TestSuite$Iface", providerInfo.get("serviceInfo").get(0).get("serviceIfaceInfos").get(0).get("ifaceName").asText());
            Assert.assertEquals("com.meituan.dorado.test.thrift.apisuite.TestSuiteImpl", providerInfo.get("serviceInfo").get(0).get("serviceIfaceInfos").get(0).get("implName").asText());
        }

        // 2. Assert the provider method.info path
        {
            String url = String.format("http://localhost:%d/method.info", port);
            String result = HttpClientUtil.doGet(url, null);

            JsonNode methodInfo = null;
            try {
                methodInfo = new ObjectMapper().readTree(result);
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
            Assert.assertEquals(3, methodInfo.size());
            Assert.assertEquals("com.meituan.octo.dorado.server", methodInfo.get("appkey").asText());
            Assert.assertEquals(1, methodInfo.get("serviceMethods").size());
            JsonNode serviceMethod = methodInfo.get("serviceMethods").get(0);
            Assert.assertEquals("com.meituan.dorado.test.thrift.apisuite.TestSuite", serviceMethod.get("serviceName").asText());

            System.out.println(serviceMethod.get("methods"));
            Assert.assertEquals(16, serviceMethod.get("methods").size());
            final Set<String> expectedMethods = Sets.newHashSet(methods);
            for (JsonNode node : serviceMethod.get("methods")) {
                Assert.assertTrue(expectedMethods.remove(node.asText()));
            }
            Assert.assertTrue(expectedMethods.isEmpty());
        }

    }

    private void assertFailureConsumerHttpCheckHandler() {
        String url = String.format("http://localhost:%d/invoker", port);
        String result = HttpClientUtil.doGet(url, null);
        Assert.assertEquals("{\"success\":false,\"result\":\"PROVIDER not support service invoke\"}", result);
    }

    private void assertSuccessConsumerHttpCheckHandler() {
        String url = String.format("http://localhost:%d/invoker", port);
        String result = HttpClientUtil.doGet(url, null);
        Assert.assertEquals("接口请求支持中", result);
    }
}
