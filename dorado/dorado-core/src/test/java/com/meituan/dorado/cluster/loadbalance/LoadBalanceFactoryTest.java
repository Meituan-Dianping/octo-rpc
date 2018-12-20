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
package com.meituan.dorado.cluster.loadbalance;


import com.meituan.dorado.cluster.LoadBalance;
import com.meituan.dorado.mock.MockLoadBalance;
import org.junit.Assert;
import org.junit.Test;

public class LoadBalanceFactoryTest {

    @Test
    public void testLoadBalance() {
        String serviceName = "testServiceName";
        Assert.assertTrue(LoadBalanceFactory.getLoadBalance(serviceName) instanceof RandomLoadBalance);

        LoadBalanceFactory.setLoadBalance(serviceName, "random");
        LoadBalance loadBalance = LoadBalanceFactory.getLoadBalance(serviceName);
        Assert.assertTrue(loadBalance instanceof RandomLoadBalance);

        LoadBalanceFactory.setCustomLoadBalance(serviceName, new MockLoadBalance());
        loadBalance = LoadBalanceFactory.getLoadBalance(serviceName);
        Assert.assertTrue(loadBalance instanceof MockLoadBalance);

        // wrong loadBalance class
        try {
            LoadBalanceFactory.setCustomLoadBalance(serviceName, new MockLoadBalance());
        } catch(Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }

        // wrong loadBalance policy
        String secondServiceName = "testSecondService";
        LoadBalanceFactory.setLoadBalance(secondServiceName, "wrong");
        Assert.assertNotNull(LoadBalanceFactory.getLoadBalance(secondServiceName) instanceof RandomLoadBalance);
    }
}
