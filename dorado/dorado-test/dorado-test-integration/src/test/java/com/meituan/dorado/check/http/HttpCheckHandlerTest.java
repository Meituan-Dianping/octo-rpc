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
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;

public class HttpCheckHandlerTest {
    private static ClassPathXmlApplicationContext serverBeanFactory;

    @BeforeClass
    public static void init() {
        serverBeanFactory = new ClassPathXmlApplicationContext("thrift/httpCheck/thrift-provider.xml");
    }

    @AfterClass
    public static void destroy() {
        serverBeanFactory.destroy();
    }

    /**
     * 测试获取服务端基本信息
     */
    @Test
    public void getServiceInfoTest() {
        String url = "http://localhost:5080/service.info";
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

}
