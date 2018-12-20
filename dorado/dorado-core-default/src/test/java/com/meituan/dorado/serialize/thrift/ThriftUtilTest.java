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
package com.meituan.dorado.serialize.thrift;


import com.meituan.dorado.Echo;
import com.meituan.dorado.common.exception.ProtocolException;
import com.meituan.dorado.transport.meta.DefaultRequest;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

public class ThriftUtilTest {

    @Test
    public void test() {
        String methodName = ThriftUtil.generateBoolMethodName("test");
        Assert.assertEquals(methodName, "isTest");

        methodName = ThriftUtil.generateSetMethodName("test");
        Assert.assertEquals(methodName, "setTest");

        methodName = ThriftUtil.generateMethodArgsClassName("testService", "testMethod");
        Assert.assertEquals(methodName, "testService&testMethod_args");

        methodName = ThriftUtil.generateMethodArgsClassName("test$inner", "testMethod");
        Assert.assertEquals(methodName, "test$testMethod_args");

        methodName = ThriftUtil.generateMethodResultClassName("testService", "testMethod");
        Assert.assertEquals(methodName, "testService&testMethod_result");

        methodName = ThriftUtil.generateMethodResultClassName("test$inner", "testMethod");
        Assert.assertEquals(methodName, "test$testMethod_result");

        Method method = ThriftUtil.obtainGetMethod(DefaultRequest.class, "serviceImpl");
        Assert.assertEquals(method.getName(), "getServiceImpl");

        try {
            ThriftUtil.obtainGetMethod(DefaultRequest.class, "mockField");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof ProtocolException);
        }

        Assert.assertTrue(!ThriftUtil.isSupportedThrift(DefaultRequest.class));
        Assert.assertTrue(ThriftUtil.isSupportedThrift(Echo.class));
    }
}
