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

import com.meituan.dorado.common.exception.TransportException;
import com.meituan.dorado.util.ClazzUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractChannel implements Channel {

    private static final Logger logger = LoggerFactory.getLogger(AbstractChannel.class);

    private volatile boolean closed;

    @Override
    public void send(Object message) throws TransportException {
        String messageName = message == null ? "" : ClazzUtil.getClazzSimpleName(message.getClass());
        if (isClosed()) {
            logger.warn("Channel{} closed, won't send {}", this.toString(), messageName);
            return;
        }
        if (!isConnected()) {
            throw new TransportException(
                    "Failed to send message " + messageName + ", cause: Channel" + this + " is not connected.");

        }
        doSend(message);
    }

    protected abstract void doSend(Object message);

    @Override
    public String toString() {
        return getLocalAddress() + " -> " + getRemoteAddress();
    }

    @Override
    public void close() {
        closed = true;
        doClose();
    }

    protected abstract void doClose();

    @Override
    public boolean isClosed() {
        return closed;
    }
}
