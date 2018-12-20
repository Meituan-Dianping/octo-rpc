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
package com.meituan.dorado.registry;

import com.google.common.collect.Sets;
import com.meituan.dorado.registry.meta.RegistryInfo;
import com.meituan.dorado.registry.meta.SubscribeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class RegistryPolicy implements RegistryService, DiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(RegistryPolicy.class);

    protected Registry registry;

    protected final Set<RegistryInfo> registered = Sets.newConcurrentHashSet();

    protected final ConcurrentMap<SubscribeInfo, ProviderListener> subscribed = new ConcurrentHashMap<>();

    private Map<String, String> attachments = Collections.emptyMap();

    public RegistryPolicy(Registry registry) {
        this.registry = registry;
    }

    public Map<String, String> getAttachments() {
        return attachments;
    }

    public void setAttachments(Map<String, String> attachments) {
        this.attachments = attachments;
    }

    @Override
    public void register(RegistryInfo info) {
        registered.add(info);
        doRegister(info);
        logger.info("Register {} by {}", info.toString(), registry.getClass().getName());
    }

    @Override
    public void unregister(RegistryInfo info) {
        registered.remove(info);
        doUnregister(info);
        logger.info("Unregister {} by {}", info.toString(), registry.getClass().getName());
    }

    @Override
    public void subcribe(SubscribeInfo info, ProviderListener listener) {
        subscribed.put(info, listener);
        doSubcribe(info, listener);
        logger.info("Subscribe {} by {}", info.toString(), registry.getClass().getName());
    }

    @Override
    public void unsubcribe(SubscribeInfo info) {
        subscribed.remove(info);
        doUnsubcribe(info);
        logger.info("Unsubscribe {} by {}", info.toString(), registry.getClass().getName());
    }

    public void reRegistry() {
        for (RegistryInfo info : registered) {
            logger.info("Re-register: {} by {}", info.toString(), registry.getClass().getName());
            doRegister(info);
        }
    }

    public void reSubscribe() {
        for (SubscribeInfo info : subscribed.keySet()) {
            logger.info("Re-subscribe: {} by {}", info.toString(), registry.getClass().getName());
            doSubcribe(info, subscribed.get(info));
        }
    }

    @Override
    public void setRegistryPolicy(RegistryPolicy registryPolicy) {
        // do nothing
    }

    protected abstract void doRegister(RegistryInfo info);

    protected abstract void doUnregister(RegistryInfo info);

    protected abstract void doSubcribe(SubscribeInfo info, ProviderListener listener);

    protected abstract void doUnsubcribe(SubscribeInfo info);

}
