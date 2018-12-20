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
package com.meituan.dorado.registry.zookeeper.util;

import com.meituan.dorado.registry.meta.Provider;
import com.meituan.dorado.registry.meta.SubscribeInfo;
import com.meituan.dorado.registry.zookeeper.RegistryTest;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class ZooKeeperNodeInfoTest extends RegistryTest {

    @Test
    public void testNodeInfo() {
        try {
            String nodeJson = ZooKeeperNodeInfo.genNodeData(genRegistryInfo());
            Provider provider = ZooKeeperNodeInfo.genProvider(nodeJson, genSubscribeInfo());
            Assert.assertEquals(9001, provider.getPort());

            nodeJson = ZooKeeperNodeInfo.genNodeData(genRegistryInfo());
            provider = ZooKeeperNodeInfo.genProvider(nodeJson, genDiffServiceNameSubscribeInfo());
            Assert.assertNull(provider);

        } catch (IOException e) {
            Assert.fail();
        }

    }

    public SubscribeInfo genDiffServiceNameSubscribeInfo() {
        SubscribeInfo mnsSubscribeInfo = new SubscribeInfo();
        mnsSubscribeInfo.setLocalAppkey("com.meituan.octo.dorado.client");
        mnsSubscribeInfo.setRemoteAppkey("com.meituan.octo.dorado.server");
        mnsSubscribeInfo.setServiceName("com.meituan.mtthrift.test.Hello");
        mnsSubscribeInfo.setProtocol("thrift");

        return mnsSubscribeInfo;
    }
}