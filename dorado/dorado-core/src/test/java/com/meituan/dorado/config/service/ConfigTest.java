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
package com.meituan.dorado.config.service;


import com.meituan.dorado.HelloService;
import com.meituan.dorado.MockUtil;
import org.junit.Assert;
import org.junit.Test;

public class ConfigTest {

    @Test
    public void testReferenceConfig() {
        ReferenceConfig config = MockUtil.getReferenceConfig();

        try {
            config.init();
            Assert.assertNotNull(config.get());
            Assert.assertNotNull(config.getSyncIfaceInterface(HelloService.class));

            config.destroy();
            config.destroy();
            config.get();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalStateException);
        }
    }


    @Test
    public void testInitAndDestroy() {

        ProviderConfig config = MockUtil.getProviderConfig();
        try {
            config.init();
            Assert.assertTrue(config.getServiceList().size() == 2);

            config.destroy();
            config.destroy();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalStateException);
        }
    }
}
