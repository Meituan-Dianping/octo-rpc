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
package com.meituan.dorado.rpc.handler.invoker;


import com.meituan.dorado.MockUtil;
import com.meituan.dorado.mock.MockInvoker;
import com.meituan.dorado.registry.meta.Provider;
import org.junit.Assert;
import org.junit.Test;

public class AbstractInvokerTest {

    @Test
    public void test() {
        MockInvoker invoker = MockUtil.getInvoker();
        try {
            invoker.invoke(MockUtil.getRpcInvocation());
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        Assert.assertTrue(invoker.getProvider().getWeight() == 10);
        Provider provider = MockUtil.getProvider();
        provider.setWeight(5);
        invoker.updateProviderIfNeeded(provider);
        Assert.assertTrue(invoker.getProvider().getWeight() == 5);

        Assert.assertNotNull(invoker.getClient());
        invoker.destroy();
        Assert.assertNull(invoker.getClient());
        Assert.assertTrue(invoker.isDestroyed());
    }
}
