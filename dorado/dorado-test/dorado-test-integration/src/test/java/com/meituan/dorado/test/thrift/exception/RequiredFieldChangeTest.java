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
package com.meituan.dorado.test.thrift.exception;

import com.meituan.dorado.common.exception.ProtocolException;
import com.meituan.dorado.test.thrift.exception.api.ApiVersion1;
import com.meituan.dorado.test.thrift.exception.api.ApiVersion2;
import com.meituan.dorado.test.thrift.exception.api.Result2;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 测试调用端IDL类和服务端不一致的情况
 * <p>
 * 此测试的result required字段有差异
 * <p>
 * api1.Result比api2.Result多一个 required 字段
 */
public class RequiredFieldChangeTest {

    private static final Logger logger = LoggerFactory.getLogger(RequiredFieldChangeTest.class);

    private static ClassPathXmlApplicationContext beanFactory;
    private static ApiVersion1.Iface client1;
    private static ApiVersion2.Iface client2;


    @BeforeClass
    public static void start() {
        beanFactory = new ClassPathXmlApplicationContext("thrift/exception/fieldchange/thrift-fieldchange.xml");
        client1 = (ApiVersion1.Iface) beanFactory.getBean("client1");
        client2 = (ApiVersion2.Iface) beanFactory.getBean("client2");
    }

    @AfterClass
    public static void stop() {
        beanFactory.destroy();
    }

    /**
     * 调用端 解码 时校验异常
     * <p>
     * 服务端的返回缺失了一个required字段
     */
    @Test
    public void testClient1() {
        try {
            client1.send("message", "param");
            Assert.fail();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertTrue(e.getCause() instanceof ProtocolException);
        }
    }

    /**
     * 请求正常
     * <p>
     * 服务端的返回多了一个required字段，对调用端解码无影响
     */
    @Test
    public void testClient2() {
        try {
            Result2 result = client2.send("message", "param");
            logger.info("EchoResult2: " + result);
        } catch (Exception e) {
            logger.error("Failed", e);
            Assert.fail();
        }
    }
}
