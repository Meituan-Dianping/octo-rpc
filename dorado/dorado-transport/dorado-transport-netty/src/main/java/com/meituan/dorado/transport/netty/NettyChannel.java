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

import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.exception.TransportException;
import com.meituan.dorado.rpc.ResponseFuture;
import com.meituan.dorado.transport.AbstractChannel;
import com.meituan.dorado.transport.meta.Request;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class NettyChannel extends AbstractChannel {

    private static final Logger logger = LoggerFactory.getLogger(NettyChannel.class);

    private io.netty.channel.Channel ioChannel;

    public NettyChannel(io.netty.channel.Channel channel) {
        this.ioChannel = channel;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return ((InetSocketAddress) ioChannel.remoteAddress());
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) ioChannel.localAddress();
    }

    @Override
    public boolean isConnected() {
        return ioChannel != null && ioChannel.isActive();
    }

    @Override
    public void doSend(final Object message) {
        try {
            ioChannel.writeAndFlush(message).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture f) {
                    if (!f.isSuccess()) {
                        logger.error("Failed to send message[{}] to {}.", message.toString(), getRemoteAddress(), f.cause());
                        if (message instanceof Request) {
                            final Request request = (Request) message;
                            ResponseFuture future = (ResponseFuture) request.getAttachment(Constants.RESPONSE_FUTURE);
                            if (future != null) {
                                future.setCause(new TransportException("Failed to send message " + message +
                                        " to " + getRemoteAddress(), f.cause()));
                            }
                        }
                    }
                }
            });
        } catch (Throwable e) {
            throw new TransportException("Failed to send message " + message + " to " + getRemoteAddress() + ", caused by " + e.getMessage(), e);
        }
    }

    @Override
    public void doClose() {
        try {
            logger.info("Closing netty channel {}", ioChannel.toString());
            ChannelFuture future = ioChannel.close();
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    if (!future.isSuccess()) {
                        logger.warn("Netty channel close failed", future.cause());
                        if (ioChannel.isActive()) {
                            ioChannel.close();
                        }
                    }
                    ChannelManager.removeIfDisconnected(ioChannel);
                }
            });
        } catch (Exception e) {
            logger.warn("Netty channel close failed", e);
        }
    }

    @Override
    public String toString() {
        return ioChannel == null ? "ioChannel is not initialized" : ioChannel.toString();
    }
}
