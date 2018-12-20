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

import com.meituan.dorado.common.extension.ExtensionLoader;
import com.meituan.dorado.config.service.ClientConfig;
import com.meituan.dorado.config.service.ProviderConfig;
import com.meituan.dorado.registry.meta.Provider;
import com.meituan.dorado.rpc.handler.invoker.DoradoInvoker;
import com.meituan.dorado.transport.Client;
import com.meituan.dorado.transport.ClientFactory;
import com.meituan.dorado.transport.Server;
import com.meituan.dorado.transport.ServerFactory;
import io.netty.channel.Channel;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;

public class NettyClientTest extends NettyTest {

    private static ServerFactory serverFactory;
    private static ClientFactory clientFactory;

    @BeforeClass
    public static void init() {
        serverFactory = ExtensionLoader.getExtension(ServerFactory.class);
        clientFactory = ExtensionLoader.getExtension(ClientFactory.class);
    }

    @Test
    public void clientConnectServerTest() {
        try {
            ProviderConfig providerConfig = genProviderConfig();
            ClientConfig clientConfig = genClientConfig();

            Server server = serverFactory.buildServer(providerConfig);
            Client client = clientFactory.buildClient(clientConfig, new DoradoInvoker(clientConfig, new Provider(IP, PORT)));

            client.reconnect();
            server.close();
            client.close();

            Field channelMapField = ChannelManager.class.getDeclaredField("channelPair");
            channelMapField.setAccessible(true);
            Thread.sleep(1000);

            ConcurrentMap<Channel, NettyChannel> channelMap = (ConcurrentMap<Channel, NettyChannel>) channelMapField.get(ChannelManager.class);
            Assert.assertTrue(channelMap.isEmpty());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

}
