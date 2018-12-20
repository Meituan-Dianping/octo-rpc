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

import java.net.InetSocketAddress;

public interface Channel {

    /**
     * 获取连接远端地址
     *
     * @return
     */
    InetSocketAddress getRemoteAddress();

    /**
     * 获取连接本地地址
     *
     * @return
     */
    InetSocketAddress getLocalAddress();

    /**
     * 判断连接是否可用
     *
     * @return
     */
    boolean isConnected();

    /**
     * 关闭连接
     */
    void close();

    /**
     * 判断连接是否已经关闭
     *
     * @return
     */
    boolean isClosed();

    /**
     * 发送数据
     *
     * @param message
     */
    void send(Object message) throws TransportException;
}