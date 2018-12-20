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


import com.meituan.dorado.transport.AbstractChannel;

import java.net.InetSocketAddress;

public class MockChannel extends AbstractChannel {

    @Override
    protected void doSend(Object message) {}

    @Override
    protected void doClose() {}

    @Override
    public InetSocketAddress getRemoteAddress() {
        return new InetSocketAddress("4.3.2.1", 1000);
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return new InetSocketAddress("1.2.3.4", 1000);
    }

    @Override
    public boolean isConnected() {
        return false;
    }
}
