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


import com.meituan.dorado.rpc.handler.invoker.Invoker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 加权轮询，同Nginx加权轮询, 区别是对于失败节点权重未做变更
 */
public class RoundRobinLoadBalance extends AbstractLoadBalance {

    private final ConcurrentMap<String, ConcurrentMap<Invoker, Double>> svcCurrWeightMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicInteger> sequences = new ConcurrentHashMap<>();

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers) {
        int length = invokers.size();
        double totalWeight = 0;
        double maxWeight = 0;
        double minWeight = Double.MAX_VALUE;
        Map<Invoker, Double> effectiveWeightMap = new HashMap<>();

        for (int i = 0; i < length; i++) {
            Invoker invoker = invokers.get(i);
            double weight = getWeight(invoker);
            maxWeight = Math.max(maxWeight, weight);
            minWeight = Math.min(minWeight, weight);
            effectiveWeightMap.put(invoker, weight);
            totalWeight += weight;
        }

        String interfaceName = invokers.get(0).getInterface().getName();
        AtomicInteger sequence = sequences.get(interfaceName);
        if (sequence == null) {
            sequences.putIfAbsent(interfaceName, new AtomicInteger());
            sequence = sequences.get(interfaceName);
        }
        int currSequence = sequence.incrementAndGet();
        if (Double.compare(totalWeight, 0) == 0 || Double.compare(maxWeight, minWeight) == 0) {
            return invokers.get(currSequence % length);
        }

        double currMaxWeight = 0;
        int selectedIndex = 0;
        ConcurrentMap<Invoker, Double> currWeightMap = svcCurrWeightMap.get(interfaceName);
        if (currWeightMap == null) {
            svcCurrWeightMap.putIfAbsent(interfaceName, new ConcurrentHashMap<Invoker, Double>());
            currWeightMap = svcCurrWeightMap.get(interfaceName);
        }
        Invoker selectedInvoker;
        synchronized (currWeightMap) {
            for (int i = 0; i < length; i++) {
                Invoker invoker = invokers.get(i);
                Double currentWeight = currWeightMap.get(invoker);
                Double effectiveWeight = effectiveWeightMap.get(invoker);
                if (Double.compare(effectiveWeight, 0) <= 0) {
                    continue;
                }
                if (currentWeight == null) {
                    currWeightMap.putIfAbsent(invoker, 0.0);
                    currentWeight = currWeightMap.get(invoker);
                }
                currentWeight += effectiveWeight;
                currWeightMap.put(invoker, currentWeight);
                if (Double.compare(currentWeight, currMaxWeight) > 0) {
                    currMaxWeight = currentWeight;
                    selectedIndex = i;
                }
            }

            selectedInvoker = invokers.get(selectedIndex);
            currWeightMap.put(selectedInvoker, currWeightMap.get(selectedInvoker) - totalWeight);
        }
        return selectedInvoker;
    }
}
