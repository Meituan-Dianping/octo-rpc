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

import com.meituan.dorado.common.util.NetUtil;
import com.meituan.dorado.transport.Channel;
import com.meituan.dorado.transport.ChannelHandler;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@io.netty.channel.ChannelHandler.Sharable
public class NettyServerHandler extends ChannelDuplexHandler {

    private final ChannelHandler handler;
    private final ConcurrentMap<String, Channel> clientChannels = new ConcurrentHashMap<>();

    public NettyServerHandler(ChannelHandler handler) {
        this.handler = handler;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            NettyChannel nettyChannel = ChannelManager.getOrAddChannel(ctx.channel());

            handler.received(nettyChannel, msg);
        } finally {
            ChannelManager.removeIfDisconnected(ctx.channel());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        try {
            NettyChannel channel = ChannelManager.getOrAddChannel(ctx.channel());
            handler.exceptionCaught(channel, cause);
        } finally {
            ChannelManager.removeIfDisconnected(ctx.channel());
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        try {
            NettyChannel nettyChannel = ChannelManager.getOrAddChannel(ctx.channel());
            clientChannels.put(NetUtil.toIpPort((InetSocketAddress) ctx.channel().remoteAddress()), nettyChannel);
        } finally {
            ChannelManager.removeIfDisconnected(ctx.channel());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ChannelManager.removeIfDisconnected(ctx.channel());
        clientChannels.remove(NetUtil.toIpPort((InetSocketAddress) ctx.channel().remoteAddress()));
    }

    /**
     * @param ctx
     * @param future
     * @throws Exception
     */
    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise future) throws Exception {
        ctx.close();
        ChannelManager.removeIfDisconnected(ctx.channel());
    }

    public ConcurrentMap<String, Channel> getChannels() {
        return clientChannels;
    }

}
