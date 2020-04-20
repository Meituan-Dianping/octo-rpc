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

import com.meituan.dorado.common.exception.TimeoutException;
import com.meituan.dorado.test.thrift.api.Echo2;
import com.meituan.dorado.test.thrift.filter.ClientQpsLimitFilter;
import com.meituan.dorado.test.thrift.filter.ServerQpsLimitFilter;
import org.junit.*;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class MistakeInterfaceNameTest {

    private static ClassPathXmlApplicationContext beanFactory;

    @BeforeClass
    public static void init() {
        beanFactory = new ClassPathXmlApplicationContext("thrift/exception/thrift-mistake-interface.xml");
        ClientQpsLimitFilter.disable();
        ServerQpsLimitFilter.disable();
    }

    /**
     * octo协议会校验接口，将失败
     * TODO 改为返回明确异常
     */
    @Test
    public void testOCTOProtocol() {
        Echo2.Iface echo = (Echo2.Iface) beanFactory.getBean("octoProtocolEcho");
        try {
            echo.echo("Hello world");
            Assert.fail();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertTrue(e instanceof TimeoutException);
        }
    }

    /**
     * 原生thrift协议不关注接口信息，不会失败
     */
    @Test
    public void testOldProtocol() {
        Echo2.Iface echo = (Echo2.Iface) beanFactory.getBean("oldProtocolEcho");
        try {
            String result = echo.echo("Hello world");
            Assert.assertEquals("echo: Hello world", result);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @AfterClass
    public static  void stop() {
        beanFactory.destroy();
    }
}
