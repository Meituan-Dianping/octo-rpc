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

import com.meituan.dorado.common.exception.TransportException;
import io.netty.channel.Channel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ChannelManager {

    // <ip:port <ChannelPair>>
    private static final ConcurrentMap<Channel, NettyChannel> channelPair = new ConcurrentHashMap<>();

    public static NettyChannel getOrAddChannel(Channel ioChannel) {
        if (ioChannel == null) {
            throw new TransportException("Channel is null");
        }

        NettyChannel channel = channelPair.get(ioChannel);
        if (channel == null) {
            NettyChannel nettyChannel = new NettyChannel(ioChannel);
            channelPair.putIfAbsent(ioChannel, nettyChannel);
            channel = channelPair.get(ioChannel);
        }
        return channel;
    }

    public static void removeIfDisconnected(Channel ioChannel) {
        if (ioChannel != null && !ioChannel.isActive()) {
            channelPair.remove(ioChannel);
        }
    }
}
