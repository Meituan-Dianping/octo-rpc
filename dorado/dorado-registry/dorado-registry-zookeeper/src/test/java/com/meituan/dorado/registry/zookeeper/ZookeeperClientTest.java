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
import com.meituan.dorado.common.extension.ExtensionLoader;
import com.meituan.dorado.registry.RegistryFactory;
import com.meituan.dorado.registry.meta.Provider;
import com.meituan.dorado.registry.meta.RegistryInfo;
import com.meituan.dorado.registry.zookeeper.curator.NodeChangeListener;
import com.meituan.dorado.registry.zookeeper.curator.ZookeeperClient;
import com.meituan.dorado.registry.zookeeper.curator.ZookeeperManager;
import com.meituan.dorado.registry.zookeeper.util.ZooKeeperNodeInfo;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

public class ZookeeperClientTest extends RegistryTest {

    private static ZookeeperClient zkClient;

    @BeforeClass
    public static void init() throws Exception {
        initZkServer();
        zkClient = ZookeeperManager.getZkClient(address);
    }

    @AfterClass
    public static void stop() throws Exception {
        ZookeeperManager.closeZkClient(address);
        zkServer.stop();
    }

    @Test
    public void createNode() {
        RegistryInfo registryInfo = genRegistryInfo();
        String root = "/dorado/com.meituan.octo.dorado.server/provider";
        String child = registryInfo.getIp() + Constants.COLON + registryInfo.getPort();
        String path = root + "/" + child;
        try {
            Assert.assertEquals(false, zkClient.checkExists(path));
            zkClient.createNode(path, ZooKeeperNodeInfo.genNodeData(genRegistryInfo()));
            Assert.assertEquals(true, zkClient.checkExists(path));
            zkClient.delete(path);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void delete() {
        RegistryInfo registryInfo = genRegistryInfo();
        String root = "/dorado/com.meituan.octo.dorado.server/provider";
        String child = registryInfo.getIp() + Constants.COLON + registryInfo.getPort();
        String path = root + "/" + child;
        try {
            Assert.assertEquals(false, zkClient.checkExists(path));
            zkClient.createNode(path, ZooKeeperNodeInfo.genNodeData(genRegistryInfo()));
            Assert.assertEquals(true, zkClient.checkExists(path));

            zkClient.delete(path);
            Assert.assertEquals(false, zkClient.checkExists(path));
            Assert.assertEquals(true, zkClient.checkExists(root));
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void updateNodeData() {
        RegistryInfo registryInfo = genRegistryInfo();
        String root = "/dorado/com.meituan.octo.dorado.server/provider";
        String child = registryInfo.getIp() + Constants.COLON + registryInfo.getPort();
        String path = root + "/" + child;
        try {
            List<String> childNodes = zkClient.getChildren(root);
            Assert.assertEquals(0, childNodes.size());
            zkClient.createNode(path, ZooKeeperNodeInfo.genNodeData(registryInfo));
            childNodes = zkClient.getChildren(root);
            Assert.assertEquals(1, childNodes.size());
            Assert.assertEquals(child, childNodes.get(0));

            String nodeData = zkClient.getNodeData(path);
            Provider provider = ZooKeeperNodeInfo.genProvider(nodeData, genSubscribeInfo());
            Assert.assertTrue(Double.compare(registryInfo.getWeight(), provider.getWeight()) == 0);

            // 修改权重
            int newWeight = 1;
            registryInfo.setWeight(newWeight);
            zkClient.updateNodeData(path, ZooKeeperNodeInfo.genNodeData(registryInfo));
            nodeData = zkClient.getNodeData(path);
            provider = ZooKeeperNodeInfo.genProvider(nodeData, genSubscribeInfo());
            Assert.assertTrue(Double.compare(newWeight, provider.getWeight()) == 0);

            zkClient.delete(path);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void addChildNodeChangeListener() {
        try {
            ZookeeperDiscoveryService discoveryService = (ZookeeperDiscoveryService) ExtensionLoader.getExtensionWithName(RegistryFactory.class, "zookeeper")
                    .getDiscoveryService(address);

            RegistryInfo registryInfo = genRegistryInfo();
            String root = "/dorado/com.meituan.octo.dorado.server/provider";
            String child = registryInfo.getIp() + Constants.COLON + registryInfo.getPort();
            String path = root + "/" + child;

            zkClient.createNode(path, ZooKeeperNodeInfo.genNodeData(registryInfo));

            MockNotifyListener notifyListener = new MockNotifyListener();
            NodeChangeListener listener = discoveryService.buildNodeChangeListener(genSubscribeInfo(), notifyListener);
            zkClient.addChildNodeChangeListener(root, listener);

            Thread.sleep(300);
            List<Provider> providers = notifyListener.getProviders();
            Assert.assertNotNull(providers);
            Assert.assertEquals(1, providers.size());
            Assert.assertEquals(registryInfo.getAppkey(), providers.get(0).getAppkey());
            Assert.assertTrue(Double.compare(registryInfo.getWeight(), providers.get(0).getWeight()) == 0);

            // 修改权重
            int newWeight = 1;
            registryInfo.setWeight(newWeight);
            zkClient.updateNodeData(path, ZooKeeperNodeInfo.genNodeData(registryInfo));
            Thread.sleep(300);
            providers = notifyListener.getProviders();
            Assert.assertNotNull(providers);
            Assert.assertEquals(1, providers.size());
            Assert.assertTrue(Double.compare(registryInfo.getWeight(), providers.get(0).getWeight()) != 0);

            // 删除节点
            zkClient.delete(path);
            Thread.sleep(300);
            providers = notifyListener.getProviders();
            Assert.assertEquals(0, providers.size());

            discoveryService.destroy();
        } catch (Exception e) {
            Assert.fail();
        }
    }
}