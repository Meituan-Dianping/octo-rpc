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
package com.meituan.dorado.registry.zookeeper;

import com.meituan.dorado.common.exception.RegistryException;
import com.meituan.dorado.registry.DiscoveryService;
import com.meituan.dorado.registry.RegistryService;
import com.meituan.dorado.registry.support.AbstractRegistryFactory;
import org.apache.commons.lang3.StringUtils;

public class ZookeeperRegistryFactory extends AbstractRegistryFactory {

    private final String NAME = "zookeeper";

    @Override
    public RegistryService getRegistryService(String address) {
        if (StringUtils.isBlank(address)) {
            throw new RegistryException("ZookeeperRegistry lack of address");
        }
        return new ZookeeperRegistryService(address);
    }

    @Override
    public DiscoveryService getDiscoveryService(String address) {
        if (StringUtils.isBlank(address)) {
            throw new RegistryException("ZookeeperRegistry lack of address");
        }
        return new ZookeeperDiscoveryService(address);
    }

    @Override
    public String getName() {
        return NAME;
    }
}
