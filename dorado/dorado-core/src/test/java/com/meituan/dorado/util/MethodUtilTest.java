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
package com.meituan.dorado.util;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

public class MethodUtilTest {

    @Test
    public void testGenerateMethodSignatureByMethod() {
        Method[] method = MethodUtilTest.class.getMethods();
        String methodSign = MethodUtil.generateMethodSignatureByMethod(this.getClass(), method[0]);
        Assert.assertTrue("com.meituan.dorado.util.MethodUtilTest:testGenerateMethodSignatureByMethod()".equals(methodSign));
    }

    @Test
    public void testGenerateMethodSignature() {
        Class<?> clazz = this.getClass();
        String methodName = "generateMethodSignature";
        Class<?>[] parameterTypes = {String.class, Class[].class};
        String methodSign = MethodUtil.generateMethodSignature(clazz, methodName, parameterTypes);
        Assert.assertTrue("com.meituan.dorado.util.MethodUtilTest:generateMethodSignature(java.lang.String, [Ljava.lang.Class;)".equals(methodSign));
    }
}
