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
package com.meituan.dorado.cluster;


import com.meituan.dorado.MockUtil;
import com.meituan.dorado.config.service.ClientConfig;
import com.meituan.dorado.mock.MockRegistry;
import com.meituan.dorado.registry.support.FailbackRegistry;
import org.junit.Assert;
import org.junit.Test;

public class InvokerRepositoryTest {

    @Test
    public void testNotify() {

        ClientConfig clientConfig = MockUtil.getClientConfig();
        InvokerRepository invokerRepository = new InvokerRepository(clientConfig);
        Assert.assertEquals("mock", invokerRepository.getClientConfig().getProtocol());
        Assert.assertNull(invokerRepository.getRegistry());

        invokerRepository.notify(MockUtil.getProviderList());
        Assert.assertTrue(invokerRepository.getInvokers().size() == 3);

        invokerRepository.setRegistry(new FailbackRegistry(new MockRegistry()));
        Assert.assertTrue(invokerRepository.getRegistry() instanceof FailbackRegistry);

        // direct conn
        invokerRepository = new InvokerRepository(clientConfig, true);
        Assert.assertTrue(invokerRepository.getInvokers().size() == 1);
        Assert.assertTrue(invokerRepository.isDirectConn());

        // clear operation
        invokerRepository.destroy();
        Assert.assertTrue(invokerRepository.getInvokers().size() == 0);
    }
}
