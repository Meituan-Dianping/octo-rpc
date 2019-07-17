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
package com.meituan.dorado.registry.support;


import com.google.common.collect.Sets;
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.exception.RegistryException;
import com.meituan.dorado.common.thread.DefaultThreadFactory;
import com.meituan.dorado.registry.*;
import com.meituan.dorado.registry.meta.RegistryInfo;
import com.meituan.dorado.registry.meta.SubscribeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.*;

public class FailbackRegistry extends RegistryPolicy {

    private final Logger logger = LoggerFactory.getLogger(FailbackRegistry.class);

    // 定时任务执行器
    private final ScheduledExecutorService retryExecutor = Executors.newScheduledThreadPool(1, new DefaultThreadFactory("DoradoRegistryFailedRetryTimer", true));
    // 失败重试定时器，定时检查是否有请求失败，如有，无限次重试
    private final ScheduledFuture<?> retryFuture;

    private final Set<RegistryInfo> failedRegistered = Sets.newConcurrentHashSet();

    private final Set<RegistryInfo> failedUnregistered = Sets.newConcurrentHashSet();

    private final ConcurrentMap<SubscribeInfo, ProviderListener> failedSubscribed = new ConcurrentHashMap<SubscribeInfo, ProviderListener>();

    private final Set<SubscribeInfo> failedUnsubscribed = Sets.newConcurrentHashSet();

    public FailbackRegistry(Registry registry) {
        super(registry);
        this.retryFuture = retryExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                // 检测并连接注册中心
                try {
                    retry();
                } catch (Throwable t) { // 防御性容错
                    logger.error("Unexpected error occur at failed retry", t);
                }
            }
        }, Constants.DEFAULT_REGISTRY_RETRY_PERIOD, Constants.DEFAULT_REGISTRY_RETRY_PERIOD, TimeUnit.MILLISECONDS);
    }

    @Override
    public void doRegister(RegistryInfo info) {
        try {
            ((RegistryService) registry).register(info);
            failedRegistered.remove(info);
        } catch (ClassCastException e) {
            throw new RegistryException("Expect class " + RegistryService.class + " actual is " + registry.getClass(), e);
        } catch (Throwable e) {
            if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException) {
                throw e;
            }
            logger.error("Failed to register {}, waiting for retry", info.getServiceNames(), e);
            // 将失败的注册请求记录到失败列表，定时重试
            failedRegistered.add(info);
        }
    }

    @Override
    public void doUnregister(RegistryInfo info) {
        try {
            ((RegistryService) registry).unregister(info);
            failedUnregistered.remove(info);
        } catch (ClassCastException e) {
            throw new RegistryException("Expect class " + RegistryService.class + " actual is " + registry.getClass(), e);
        } catch (Throwable e) {
            if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException) {
                throw e;
            }
            logger.error("Failed to unregister {}, waiting for retry", info.getServiceNames(), e);
            // 将失败的注销请求记录到失败列表，定时重试
            failedUnregistered.add(info);
        }
    }

    @Override
    public void doSubcribe(SubscribeInfo info, ProviderListener listener) {
        try {
            ((DiscoveryService) registry).subscribe(info, listener);
            failedSubscribed.remove(info, listener);
        } catch (ClassCastException e) {
            throw new RegistryException("Expect class " + DiscoveryService.class + " actual is " + registry.getClass(), e);
        } catch (Throwable e) {
            if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException) {
                throw e;
            }
            logger.error("Failed to subscribe {}, waiting for retry", info.getServiceName(), e);
            // 将失败的订阅记录到失败列表，定时重试
            failedSubscribed.put(info, listener);
        }
    }

    @Override
    public void doUnsubcribe(SubscribeInfo info) {
        try {
            ((DiscoveryService) registry).unsubscribe(info);
            failedUnsubscribed.remove(info);
        } catch (ClassCastException e) {
            throw new RegistryException("Expect class " + DiscoveryService.class + " actual is " + registry.getClass(), e);
        } catch (Throwable e) {
            if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException) {
                throw e;
            }
            logger.error("Failed to unsubscribe {}, waiting for retry", info.getServiceName(), e);
            // 将失败的取消订阅请求记录到失败列表，定时重试
            failedUnsubscribed.add(info);
        }
    }

    private void retry() {
        for (RegistryInfo info : failedRegistered) {
            try {
                ((RegistryService) registry).register(info);
                failedRegistered.remove(info);
            } catch (Throwable e) {
                logger.warn("Failed to retry register {}, waiting for again, cause: {}", info.getServiceNames(), e.getMessage(), e);
            }
        }
        for (RegistryInfo info : failedUnregistered) {
            try {
                ((RegistryService) registry).unregister(info);
                failedUnregistered.remove(info);
            } catch (Throwable e) {
                logger.warn("Failed to retry unregister {}, waiting for again", info.getServiceNames(), e);
            }
        }
        for (SubscribeInfo info : failedSubscribed.keySet()) {
            try {
                ((DiscoveryService) registry).subscribe(info, failedSubscribed.get(info));
                failedSubscribed.remove(info);
            } catch (Throwable e) {
                logger.warn("Failed to retry subscribe {}, waiting for again", info.getServiceName(), e);
            }
        }
        for (SubscribeInfo info : failedUnsubscribed) {
            try {
                ((DiscoveryService) registry).unsubscribe(info);
                failedUnsubscribed.remove(info);
            } catch (Throwable e) {
                logger.warn("Failed to retry unsubscribe {}, waiting for again", info.getServiceName(), e);
            }
        }
    }

    @Override
    public void destroy() {
        try {
            retryFuture.cancel(true);
            retryExecutor.shutdown();
            registry.destroy();
        } catch (Throwable e) {
            logger.warn("FailbackRegistry destroy failed", e);
        }
    }
}
