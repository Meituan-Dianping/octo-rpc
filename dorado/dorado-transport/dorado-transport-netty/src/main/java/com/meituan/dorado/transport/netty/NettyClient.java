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
import com.meituan.dorado.config.service.ClientConfig;
import com.meituan.dorado.rpc.handler.invoker.Invoker;
import com.meituan.dorado.transport.AbstractClient;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class NettyClient extends AbstractClient {

    private static final Logger logger = LoggerFactory.getLogger(NettyClient.class);

    private static final NioEventLoopGroup WORKER_GROUP = new NioEventLoopGroup(Constants.DEFAULT_IO_WORKER_THREAD_COUNT, new DefaultThreadFactory("NettyClientWorkerGroup"));

    private Bootstrap bootstrap;
    // 目前只支持一个Channel TODO 后续考虑连接池
    private volatile NettyChannel channel;
    private int connTimeout;

    public NettyClient(ClientConfig config, Invoker invoker) {
        super(config, invoker);
    }

    @Override
    public com.meituan.dorado.transport.Channel getChannel() {
        return channel;
    }

    @Override
    protected void init() {
        connTimeout = Math.max(clientConfig.getConnTimeout(), Constants.DEFAULT_CONN_TIMEOUT);
        final NettyClientHandler clientHandler = new NettyClientHandler(getChannelHandler());
        final Map<String, Object> attachments = new HashMap<>();
        attachments.put(Constants.SERVICE_IFACE, clientConfig.getServiceInterface());
        attachments.put(Constants.TRACE_IS_RECORD_TIMELINE, clientConfig.isTimelineTrace());
        bootstrap = new Bootstrap();
        bootstrap.group(WORKER_GROUP)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connTimeout)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        NettyCodec nettyCodec = new NettyCodec(getCodec(), attachments);
                        ch.pipeline()
                                .addLast("codec", nettyCodec)
                                .addLast("handler", clientHandler);
                    }
                });
    }

    @Override
    public int getTimeout() {
        return clientConfig.getTimeout();
    }

    @Override
    protected void doConnect() {
        ChannelFuture future = null;
        if (isClosed()) {
            logger.warn("{} closed, won't do channel connect", this.toString());
        }
        try {
            future = bootstrap.connect(getRemoteAddress());
            future.awaitUninterruptibly();
            if (future.isCancelled()) {
                throw new TransportException("Failed connect to server " + getRemoteAddress() + " from " + getClass().getName() + ", cause it be cancelled");
            } else if (!future.isSuccess()) {
                String errorMsg = future.cause() != null ? future.cause().getMessage() : "";
                throw new TransportException("Failed connect to server " + getRemoteAddress() + " from " + getClass().getName() + "[timeout:" + connTimeout + "ms], cause:" + errorMsg, future.cause());
            }
            NettyChannel oldChannel = this.channel;
            NettyChannel newChannel = ChannelManager.getOrAddChannel(future.channel());
            this.channel = newChannel;

            // 关闭旧的连接
            if (oldChannel != null) {
                if (oldChannel.isConnected()) {
                    logger.info("Close old netty channel:{} and create new netty channel:{}", oldChannel, newChannel);
                    oldChannel.close();
                }
            }

        } catch (Exception e) {
            if (e instanceof TransportException) {
                throw e;
            }
            throw new TransportException("Failed to connect to server " + getRemoteAddress(), e);
        } finally {
            if (future != null && !isConnected()) {
                future.cancel(true);
            }
        }
    }

    @Override
    protected int getConnTimeout() {
        return connTimeout;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[remote:" + getRemoteAddress() + ", isClosed=" + isClosed() + "]";
    }

    @Override
    protected void doClose() {
        // NioEventLoopGroup is share with all netty client
        // cannot do shutdown
    }
}
