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
package com.meituan.dorado.mock;


import com.meituan.dorado.config.service.ClientConfig;
import com.meituan.dorado.rpc.handler.invoker.Invoker;
import com.meituan.dorado.transport.AbstractClient;
import com.meituan.dorado.transport.Channel;

import java.net.SocketException;

public class MockClient extends AbstractClient {

    private Channel channel = new MockChannel();

    private volatile boolean isConnected;

    public MockClient(ClientConfig config, Invoker invoker) {
        super(config, invoker);
    }

    @Override
    protected void init() {}

    @Override
    protected void doClose() {
        //throw new RuntimeException();
    }

    @Override
    protected int getConnTimeout() {
        return 1000;
    }

    @Override
    protected int getTimeout() {
        return 1000;
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    protected void doConnect() {
        isConnected = true;
    }

    @Override
    public boolean isConnected() {
        return isConnected;
    }
}
