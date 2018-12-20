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
package com.meituan.dorado.config.service.spring;


import com.meituan.dorado.HelloService;
import com.meituan.dorado.MockUtil;
import org.junit.Assert;
import org.junit.Test;

public class SpringBeanTest {

    @Test
    public void testSpringBean() throws Exception {
        ReferenceBean referenceBean = MockUtil.getReferenceBean();
        try {
            referenceBean.afterPropertiesSet();
        } catch (Exception e) {
            Assert.fail();
        }
        Assert.assertNotNull(referenceBean.getObject());
        Assert.assertTrue(referenceBean.getObjectType() == HelloService.class);
        Assert.assertTrue(referenceBean.isSingleton());

        ServiceBean serviceBean = MockUtil.getServerBean();
        try {
            serviceBean.afterPropertiesSet();
        } catch (Exception e) {
            Assert.fail();
        }
    }
}
