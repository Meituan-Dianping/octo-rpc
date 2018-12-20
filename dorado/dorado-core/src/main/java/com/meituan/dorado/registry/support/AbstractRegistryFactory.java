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

import com.meituan.dorado.common.Constants;
import com.meituan.dorado.registry.Registry;
import com.meituan.dorado.registry.RegistryFactory;
import com.meituan.dorado.registry.RegistryPolicy;

import java.util.Map;

public abstract class AbstractRegistryFactory implements RegistryFactory {

    private static String policyType = Constants.DEFAULT_REGISTRY_POLICY_TYPE;

    /**
     * 根据用户配置 registry, 获取注册实例
     *
     * @param registryInfo
     * @param type
     * @return
     */
    @Override
    public RegistryPolicy getRegistryPolicy(Map<String, String> registryInfo, Registry.OperType type) {
        Registry registry;
        if (Registry.OperType.REGISTRY.equals(type)) {
            registry = getRegistryService(registryInfo.get(Constants.REGISTRY_ADDRESS_KEY));
        } else {
            registry = getDiscoveryService(registryInfo.get(Constants.REGISTRY_ADDRESS_KEY));
        }
        if (registry == null) {
            throw new IllegalArgumentException("Not find registry type of " + registryInfo);
        }
        return generateRegistryPolicy(registry, registryInfo);
    }

    private static RegistryPolicy generateRegistryPolicy(Registry registry, Map<String, String> attachments) {
        RegistryPolicy registryPolicy;
        // 目前只支持重试注册容错策略
        if ("failback".equalsIgnoreCase(policyType)) {
            registryPolicy = new FailbackRegistry(registry);
        } else {
            registryPolicy = new FailbackRegistry(registry);
        }
        registryPolicy.setAttachments(attachments);
        registry.setRegistryPolicy(registryPolicy);
        return registryPolicy;
    }
}