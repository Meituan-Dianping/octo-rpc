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
import com.meituan.dorado.config.service.ProviderConfig;
import com.meituan.dorado.config.service.ServiceConfig;
import com.meituan.dorado.transport.AbstractServer;
import com.meituan.dorado.transport.Channel;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

public class NettyServer extends AbstractServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    private ServerBootstrap bootstrap;
    private ServerChannel serverChannel;
    private ConcurrentMap<String, Channel> clientChannels;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public NettyServer(ProviderConfig providerConfig)
            throws TransportException {
        super(providerConfig);
    }

    @Override
    protected void doStart() {
        if (Epoll.isAvailable()) {
            logger.info("NettyServer use EpollEventLoopGroup!");
            bossGroup = new EpollEventLoopGroup(Constants.NIO_CONN_THREADS, new DefaultThreadFactory("NettyServerBossGroup"));
            workerGroup = new EpollEventLoopGroup(providerConfig.getIoWorkerThreadCount(),
                    new DefaultThreadFactory("NettyServerWorkerGroup"));
        } else {
            bossGroup = new NioEventLoopGroup(Constants.NIO_CONN_THREADS, new DefaultThreadFactory("NettyServerBossGroup"));
            workerGroup = new NioEventLoopGroup(providerConfig.getIoWorkerThreadCount(),
                    new DefaultThreadFactory("NettyServerWorkerGroup"));
        }

        final NettyServerHandler serverHandler = new NettyServerHandler(getChannelHandler());
        clientChannels = serverHandler.getChannels();
        final Map<String, Object> attachments = new HashMap<>();
        attachments.put(Constants.SERVICE_IFACE, getDefaultServiceIface(providerConfig.getServiceConfigList()));
        attachments.put(Constants.TRACE_IS_RECORD_TIMELINE, providerConfig.isTimelineTrace());
        bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(workerGroup instanceof EpollEventLoopGroup ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        NettyCodec nettyCodec = new NettyCodec(getCodec(), attachments);
                        ch.pipeline()
                                .addLast("codec", nettyCodec)
                                .addLast("handler", serverHandler);
                    }
                });
        // bind
        ChannelFuture channelFuture = bootstrap.bind(getLocalAddress());
        channelFuture.syncUninterruptibly();
        serverChannel = (ServerChannel) channelFuture.channel();
    }

    @Override
    public boolean isBound() {
        return serverChannel.isOpen() && serverChannel.isActive();
    }

    @Override
    public void doClose() {
        try {
            if (serverChannel != null) {
                ChannelFuture future = serverChannel.close();
                future.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) {
                        if (!future.isSuccess()) {
                            logger.warn("Netty ServerChannel[{}] close failed", getLocalAddress(), future.cause());
                        }
                    }
                });
            }
        } catch (Throwable e) {
            logger.error("Netty ServerChannel close failed", e);
        }

        try {
            Set<Channel> channels = getChannels();
            for (Channel channel : channels) {
                try {
                    channel.close();
                } catch (Throwable e) {
                    logger.warn("NettyServer close channel[{}] failed", channel.getRemoteAddress(), e);
                }
            }
            clientChannels.clear();
        } catch (Throwable e) {
            logger.warn("NettyServer close channels failed", e);
        }

        try {
            if (bootstrap != null) {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        } catch (Throwable e) {
            logger.warn("NettyServer EventLoopGroup shutdown failed", e);
        }
    }

    @Override
    public Set<Channel> getChannels() {
        Set<Channel> channels = new HashSet<>();
        Iterator<Map.Entry<String, Channel>> iterator = clientChannels.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Channel> entry = iterator.next();
            Channel channel = entry.getValue();
            if (channel.isConnected()) {
                channels.add(channel);
            } else {
                iterator.remove();
            }
        }
        return channels;
    }

    private Class<?> getDefaultServiceIface(List<ServiceConfig> serviceConfigList) {
        if (serviceConfigList.size() == 1) {
            // 单端口端服务
            Class<?> serviceIface = serviceConfigList.get(0).getServiceInterface();
            return serviceIface;
        }
        return null;
    }
}
