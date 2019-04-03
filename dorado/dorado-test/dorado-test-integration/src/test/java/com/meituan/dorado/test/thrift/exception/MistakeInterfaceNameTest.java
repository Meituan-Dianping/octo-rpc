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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class MistakeInterfaceNameTest {

    ClassPathXmlApplicationContext beanFactory;

    @Before
    public void init() {
        beanFactory = new ClassPathXmlApplicationContext("thrift/exception/thrift-mistake-interface.xml");
        ClientQpsLimitFilter.disable();
        ServerQpsLimitFilter.disable();
    }

    @Test
    public void test() {
        Echo2.Iface echo = (Echo2.Iface) beanFactory.getBean("echoService");
        try {
            echo.echo("Hello world");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof TimeoutException);
        }
    }

    @After
    public void stop() {
        beanFactory.destroy();
    }
}
