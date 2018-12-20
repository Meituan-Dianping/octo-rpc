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
package com.meituan.dorado.common.util;

import org.junit.Assert;
import org.junit.Test;

public class ClassLoaderUtilTest {

    @Test
    public void testLoadClass() {
        Class<?> clazz;
        String className = "com.meituan.dorado.common.util.ClassLoaderUtil";
        try {
            clazz = ClassLoaderUtil.loadClass(className);
            Assert.assertEquals(clazz.getName(), className);
        } catch (ClassNotFoundException e) {
            Assert.fail(e.getMessage());
        }

        try {
            clazz = ClassLoaderUtil.loadClass("com.meituan.dorado.common.ClassLoadUtil");
            Assert.fail();
        } catch (ClassNotFoundException e) {
            Assert.assertEquals(e.getClass(), ClassNotFoundException.class);
        }
    }
}
