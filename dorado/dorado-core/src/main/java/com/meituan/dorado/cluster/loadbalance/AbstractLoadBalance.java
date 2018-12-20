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
import com.meituan.dorado.registry.meta.Provider;
import com.meituan.dorado.rpc.handler.invoker.Invoker;

import java.util.List;

public abstract class AbstractLoadBalance implements LoadBalance {

    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers) {
        if (invokers == null || invokers.isEmpty()) {
            return null;
        }
        if (invokers.size() == 1) {
            return invokers.get(0);
        }
        return doSelect(invokers);
    }

    protected abstract <T> Invoker<T> doSelect(List<Invoker<T>> invokers);

    protected double getWeight(Invoker<?> invoker) {
        Provider provider = invoker.getProvider();
        double weight = provider.getWeight();
        if (!provider.getWarmUpFinished().get()) {
            if (weight > 0) {
                long startTime = provider.getStartTime();
                if (startTime > 0L) {
                    int duration = (int) (System.currentTimeMillis() / 1000 - startTime);
                    int warmUpTime = provider.getWarmUp();
                    if (duration > 0 && warmUpTime > 0 && duration < warmUpTime) {
                        weight = calculateWarmUpWeight(duration, warmUpTime, weight);
                    } else if (duration >= warmUpTime) {
                        provider.getWarmUpFinished().set(true);
                    }
                }
            }
        }
        return weight;
    }

    /**
     * 如果服务提供者启动时长小于预热时间，则需要降权
     *
     * @param duration 秒
     * @param warmUpTime 秒
     * @param weight
     * @return
     */
    private static double calculateWarmUpWeight(int duration, int warmUpTime, double weight) {
        double warmUpWeight = (weight * ((double) duration / (double) warmUpTime));
        return warmUpWeight;
    }
}
