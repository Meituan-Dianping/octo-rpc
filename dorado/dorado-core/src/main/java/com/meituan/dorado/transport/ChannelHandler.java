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

public interface ChannelHandler {

    /**
     * on channel connected.
     *
     * @param channel
     */
    void connected(Channel channel);

    /**
     * on channel disconnected.
     *
     * @param channel
     */
    void disconnected(Channel channel);

    /**
     * on channel closed.
     *
     * @param channel
     */
    void closed(Channel channel);

    /**
     * on message send.
     *
     * @param channel
     * @param message
     */
    void send(Channel channel, Object message);

    /**
     * on message handle.
     *
     * @param channel
     * @param message
     */
    void received(Channel channel, Object message);

    /**
     * on exception caught.
     *
     * @param channel
     * @param exception
     */
    void exceptionCaught(Channel channel, Throwable exception);

    void destroy();
}