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
package com.meituan.dorado.degrade;


import com.meituan.dorado.MockUtil;
import com.meituan.dorado.rpc.handler.invoker.Invoker;
import org.junit.Assert;
import org.junit.Test;

public class NodeDegradeTest<T> {

    @Test
    public void testWeightChanged() {
        Invoker invoker = MockUtil.getInvoker();
        Assert.assertTrue(invoker.getProvider().getWeight() == 10);

        NodeDegrade.weightDegradeToZero(invoker, "none of your business");
        Assert.assertTrue(invoker.getProvider().getWeight() == 0);

        NodeDegrade.weightRecover(invoker);
        Assert.assertTrue(invoker.getProvider().getWeight() == 10);
    }
}
