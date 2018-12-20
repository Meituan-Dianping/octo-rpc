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
package com.meituan.dorado.transport;

import com.meituan.dorado.codec.Codec;
import com.meituan.dorado.common.exception.TransportException;
import com.meituan.dorado.common.extension.ExtensionLoader;
import com.meituan.dorado.config.service.ProviderConfig;
import com.meituan.dorado.transport.support.ProviderChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public abstract class AbstractServer implements Server {

    private static final Logger logger = LoggerFactory.getLogger(AbstractServer.class);

    protected final ProviderConfig providerConfig;
    private final ChannelHandler channelHandler;

    private final InetSocketAddress localAddress;
    private final Codec codec;

    private volatile boolean closed;

    public AbstractServer(ProviderConfig providerConfig) throws TransportException {
        this.localAddress = new InetSocketAddress(providerConfig.getPort());
        this.codec = ExtensionLoader.getExtension(Codec.class);
        this.providerConfig = providerConfig;
        this.channelHandler = new ProviderChannelHandler(providerConfig);

        start();
    }

    private void start() {
        try {
            doStart();
            logger.info("Start {} bind {}", getClass().getName(), localAddress);
        } catch (Throwable t) {
            throw new TransportException("Failed to bind " + getClass().getName()
                    + " on " + localAddress, t);
        }
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    @Override
    public void close() {
        logger.info("Closing {} bind {}", getClass().getName(), localAddress);
        try {
            doClose();
            channelHandler.destroy();
        } catch (Throwable e) {
            logger.error("{} bind {} close failed", getClass().getName(), localAddress, e);
        }
        closed = true;
    }

    protected abstract void doClose();

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public Codec getCodec() {
        return codec;
    }

    @Override
    public ChannelHandler getChannelHandler() {
        return channelHandler;
    }

    protected abstract void doStart();
}
