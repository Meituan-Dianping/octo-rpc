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


import com.meituan.dorado.MockUtil;
import com.meituan.dorado.mock.MockLoadBalance;
import com.meituan.dorado.rpc.handler.invoker.Invoker;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadBalanceTest<T> {

    @Test
    public void testRandomLoadBalance() {
        List<Invoker<T>> invokers = MockUtil.getInvokerList();
        RandomLoadBalance randomLoadBalance = new RandomLoadBalance();

        HashMap<Invoker<T>, AtomicInteger> invokerCount = new HashMap<>();
        for (int i = 0; i < 30; i++) {
            Invoker invoker = randomLoadBalance.doSelect(invokers);
            AtomicInteger count = invokerCount.get(invoker);
            if (count == null) {
                invokerCount.put(invoker, new AtomicInteger());
            }
            invokerCount.get(invoker).incrementAndGet();

        }
        System.out.println(invokerCount.values());
        Assert.assertEquals(invokerCount.size(), 4);
    }

    @Test
    public void testWeightedRoundRobinLoadBalance() throws InterruptedException {
        final List<Invoker<T>> invokers = MockUtil.getInvokerList();
        final RoundRobinLoadBalance loadBalance = new RoundRobinLoadBalance();

        List<Thread> threads = new ArrayList<>();
        final ConcurrentMap<Double, AtomicInteger> selectCount = new ConcurrentHashMap<>();
        int threadNum = 3;
        final AtomicInteger times = new AtomicInteger(0);
        for (int k = 0; k < threadNum; k++) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {

                    for (int i = 0; i < 75; i++) { // 循环次数是权重和
                        Invoker<T> selectInvoker = loadBalance.select(invokers);
                        double weight = selectInvoker.getProvider().getWeight();
                        AtomicInteger count = selectCount.get(weight);
                        if (count == null) {
                            selectCount.putIfAbsent(weight, new AtomicInteger(0));
                            count = selectCount.get(weight);
                        }
                        count.incrementAndGet();
                        times.incrementAndGet();
                    }
                }
            });
            thread.start();
            threads.add(thread);
        }
        for (Thread t : threads) {
            t.join();
        }
        Assert.assertEquals(times.get(), 75 * threadNum);
        for (Map.Entry<Double, AtomicInteger> entry : selectCount.entrySet()) {
            Assert.assertTrue(entry.getKey() * threadNum == entry.getValue().intValue());
        }
    }

    @Test
    public void testAbstractLoadBalance() {
        MockLoadBalance loadBalance = new MockLoadBalance();
        Invoker<T> invoker = loadBalance.select(new ArrayList<Invoker<T>>());
        Assert.assertNull(invoker);

        invoker = loadBalance.select(getSingleInvokerList());
        Assert.assertTrue(invoker.getProvider().getWeight() == 10);
    }

    @Test
    public void testWarmUp() throws InterruptedException {
        List<Invoker<T>> invokers = getSingleInvokerList();
        for (Invoker invoker : invokers) {
            invoker.getProvider().setStartTime(System.currentTimeMillis());
            invoker.getProvider().setWarmUp(20 * 1000);
        }

        RandomLoadBalance loadBalance = new RandomLoadBalance();
        for (int i = 0; i < 5; i++) {
            Invoker<T> invoker = loadBalance.doSelect(invokers);
            Assert.assertTrue(loadBalance.getWeight(invoker) <= 10);
            Thread.sleep(3000);
        }
        Thread.sleep(10000);
        Assert.assertTrue(loadBalance.doSelect(invokers).getProvider().getWeight() == 10);
    }

    private List<Invoker<T>> getSingleInvokerList() {
        List<Invoker<T>> invokers = new ArrayList<>();
        Invoker invoker = MockUtil.getInvoker();
        invokers.add(invoker);
        return invokers;
    }
}
