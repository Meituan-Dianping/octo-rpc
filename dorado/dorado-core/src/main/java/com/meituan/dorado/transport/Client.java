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
import com.meituan.dorado.rpc.ResponseFuture;

import java.net.InetSocketAddress;

public interface Client {

    /**
     * 重新建立连接
     */
    void reconnect();

    /**
     * 获取编解码器
     *
     * @return
     */
    Codec getCodec();

    /**
     * 获取channel
     * @return
     */
    Channel getChannel();

    /**
     * 获取handler
     *
     * @return
     */
    ChannelHandler getChannelHandler();

    /**
     * 发送请求
     *
     * @param message
     * @return
     */
    ResponseFuture request(Object message);

    /**
     * 发送请求，指定超时时间为timeout
     *
     * @param message
     * @param timeout
     * @return
     */
    ResponseFuture request(Object message, int timeout);

    InetSocketAddress getRemoteAddress();

    boolean isConnected();

    void close();

    boolean isClosed();
}