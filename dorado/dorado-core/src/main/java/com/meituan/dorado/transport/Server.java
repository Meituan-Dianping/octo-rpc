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

import java.net.InetSocketAddress;
import java.util.Set;

public interface Server {

    /**
     * 获取本地地址
     * @return
     */
    InetSocketAddress getLocalAddress();

    /**
     * 获取编解码器
     * @return
     */
    Codec getCodec();

    /**
     * 获取handler
     */
    ChannelHandler getChannelHandler();

    /**
     * 关闭server
     */
    void close();

    /**
     * 判断server是否关闭
     * @return
     */
    boolean isClosed();

    /**
     * 判断server是否已经绑定端口
     *
     * @return
     */
    boolean isBound();

    /**
     * 返回server内部的所有连接
     *
     * @return
     */
    Set<Channel> getChannels();

}