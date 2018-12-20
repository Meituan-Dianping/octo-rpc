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

import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.exception.RegistryException;
import com.meituan.dorado.registry.RegistryPolicy;
import com.meituan.dorado.registry.RegistryService;
import com.meituan.dorado.registry.meta.RegistryInfo;
import com.meituan.dorado.registry.zookeeper.curator.StateChangeListener;
import com.meituan.dorado.registry.zookeeper.curator.ZookeeperClient;
import com.meituan.dorado.registry.zookeeper.curator.ZookeeperManager;
import com.meituan.dorado.registry.zookeeper.util.ZooKeeperNodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZookeeperRegistryService implements RegistryService {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperRegistryService.class);

    private ZookeeperClient zkClient;
    private String address;
    private RegistryPolicy registryPolicy;

    public ZookeeperRegistryService(String address) {
        this.address = address;
        this.zkClient = ZookeeperManager.getZkClient(address);
        zkClient.addStateListener(new StateChangeListener() {
            @Override
            public void connStateChanged() {
                if (registryPolicy != null) {
                    registryPolicy.reRegistry();
                }
            }
        });
    }

    @Override
    public void register(RegistryInfo info) {
        try {
            String path = generateNodePath(info);
            zkClient.createNode(path, ZooKeeperNodeInfo.genNodeData(info));
            logger.info("Register provider on zookeeper[{}] path={}, info={}", address, path, info);
        } catch (Throwable e) {
            throw new RegistryException("Failed to register service: " + info.getServiceNames(), e);
        }
    }

    @Override
    public void unregister(RegistryInfo info) {
        try {
            String path = generateNodePath(info);
            zkClient.delete(path);
            logger.info("Unregister provider on zookeeper[{}] path={}, info{}", address, path, info);
        } catch (Throwable e) {
            throw new RegistryException("Failed to unregister service: " + info.getServiceNames(), e);
        }
    }

    @Override
    public void setRegistryPolicy(RegistryPolicy registryPolicy) {
        this.registryPolicy = registryPolicy;
    }

    @Override
    public void destroy() {
        ZookeeperManager.closeZkClient(address);
    }

    protected String generateNodePath(RegistryInfo info) {
        String envName = Constants.EnvType.getEnvType(info.getEnv()).getEnvName();
        StringBuilder pathBuilder = new StringBuilder(ZooKeeperNodeInfo.PATH_SEPARATOR).append(ZooKeeperNodeInfo.ROOT_NAME)
                .append(ZooKeeperNodeInfo.PATH_SEPARATOR).append(envName).append(ZooKeeperNodeInfo.PATH_SEPARATOR)
                .append(info.getAppkey()).append(ZooKeeperNodeInfo.PATH_SEPARATOR)
                .append(ZooKeeperNodeInfo.PROVIDER).append(ZooKeeperNodeInfo.PATH_SEPARATOR)
                .append(info.getIp()).append(Constants.COLON).append(info.getPort());
        return pathBuilder.toString();
    }
}
