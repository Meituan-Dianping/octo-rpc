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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomLoadBalance extends AbstractLoadBalance {

    private final Random random = new Random();

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers) {
        int length = invokers.size();
        double totalWeight = 0;
        boolean sameWeight = true;
        double lastWeight = -1;
        double[] weightArray = new double[length];

        for (int i = 0; i < length; i++) {
            double weight = getWeight(invokers.get(i));
            totalWeight += weight;
            weightArray[i] = totalWeight;
            if (sameWeight && i > 0 && Double.compare(weight, lastWeight) != 0) {
                sameWeight = false;
            }
            lastWeight = weight;
        }
        if (!sameWeight && Double.compare(totalWeight, 0) > 0) {
            double offset = random.nextDouble() * totalWeight;
            for (int i = 0; i < length; i++) {
                if (Double.compare(offset, weightArray[i]) < 0) {
                    return invokers.get(i);
                }
            }
        }
        return invokers.get(random.nextInt(length));
    }
}
