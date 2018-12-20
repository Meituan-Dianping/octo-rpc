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

import com.meituan.dorado.codec.Codec;
import com.meituan.dorado.config.service.ProviderConfig;
import com.meituan.dorado.transport.Channel;
import com.meituan.dorado.transport.ChannelHandler;
import com.meituan.dorado.transport.Server;
import com.meituan.dorado.transport.ServerFactory;

import java.net.InetSocketAddress;
import java.util.Set;

public class MockServerFactory implements ServerFactory {

    @Override
    public Server buildServer(ProviderConfig configuration) {
        return new MockServer();
    }

    public static class MockServer implements Server {

        private volatile boolean isClosed = false;

        @Override
        public InetSocketAddress getLocalAddress() {
            return null;
        }

        @Override
        public Codec getCodec() {
            return null;
        }

        @Override
        public ChannelHandler getChannelHandler() {
            return null;
        }

        @Override
        public void close() {
            isClosed = true;
        }

        @Override
        public boolean isClosed() {
            return isClosed;
        }

        @Override
        public boolean isBound() {
            return true;
        }

        @Override
        public Set<Channel> getChannels() {
            return null;
        }
    }
}
