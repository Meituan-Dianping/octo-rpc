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
package com.meituan.dorado.transport.netty;

import com.meituan.dorado.common.exception.TransportException;
import com.meituan.dorado.common.extension.ExtensionLoader;
import com.meituan.dorado.config.service.ProviderConfig;
import com.meituan.dorado.transport.Server;
import com.meituan.dorado.transport.ServerFactory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class NettyServerTest extends NettyTest {

    private static ServerFactory serverFactory;

    @BeforeClass
    public static void init() {
        serverFactory = ExtensionLoader.getExtension(ServerFactory.class);
    }

    @Test
    public void testBindPort() {
        Server server1 = null;
        Server server2 = null;
        ProviderConfig providerConfig = genProviderConfig();
        try {
            server1 = serverFactory.buildServer(providerConfig);
            Assert.assertTrue(server1.isBound());
            server2 = serverFactory.buildServer(providerConfig);
            Assert.fail();
        } catch (TransportException e) {
            // 端口占用异常要包含端口
            Assert.assertTrue(e.getMessage().contains(String.valueOf(PORT)));
        } catch (Exception e) {
            Assert.fail("Must be TransportException");
        }

        try {
            server1.close();
            Assert.assertFalse(server1.isBound());
            Assert.assertTrue(server1.isClosed());
            server2 = serverFactory.buildServer(providerConfig);
            Assert.assertTrue(server2.isBound());
            server2.close();
            server2.close();
        } catch (Exception e) {
            Assert.fail();
        }
    }
}