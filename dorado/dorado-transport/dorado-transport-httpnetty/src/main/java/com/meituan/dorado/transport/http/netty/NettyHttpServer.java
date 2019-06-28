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
package com.meituan.dorado.transport.http.netty;


import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.common.util.NetUtil;
import com.meituan.dorado.transport.http.AbstractHttpServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NettyHttpServer extends AbstractHttpServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyHttpServer.class);
    private static final int NIO_CONN_THREADS = 1;
    private static final int NIO_WORKER_THREADS = 2;

    private static volatile NettyHttpServer httpServer;
    private ServerBootstrap bootstrap;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ServerChannel serverChannel;
    private final ConcurrentMap<String, Channel> channels = new ConcurrentHashMap<String, Channel>(); // <ip:port, channel>

    public static NettyHttpServer buildHttpServer(RpcRole rpcRole) {
        if (httpServer != null) {
            httpServer.getHttpHandler().setRole(rpcRole);
            return httpServer;
        }
        synchronized (NettyHttpServer.class) {
            if (httpServer == null) {
                httpServer = new NettyHttpServer(rpcRole);
                httpServer.start();
            }
        }
        return httpServer;
    }

    public ConcurrentMap<String, Channel> getChannels() {
        return channels;
    }

    private NettyHttpServer(RpcRole role) {
        super(role);
    }

    @Override
    protected void doStart() {
        try {
            if (Epoll.isAvailable()) {
                logger.info("NettyHttpServer use EpollEventLoopGroup!");
                bossGroup = new EpollEventLoopGroup(NIO_CONN_THREADS, new DefaultThreadFactory("DoradoHttpServerBossGroup", true));
                workerGroup = new EpollEventLoopGroup(NIO_WORKER_THREADS,
                        new DefaultThreadFactory("DoradoHttpServerWorkerGroup", true));
            } else {
                bossGroup = new NioEventLoopGroup(NIO_CONN_THREADS, new DefaultThreadFactory("DoradoHttpServerBossGroup", true));
                workerGroup = new NioEventLoopGroup(NIO_WORKER_THREADS,
                        new DefaultThreadFactory("DoradoHttpServerWorkerGroup", true));
            }

            bootstrap = new ServerBootstrap();
            final NettyHttpServer httpServer = this;
            bootstrap.group(bossGroup, workerGroup)
                    .channel(workerGroup instanceof EpollEventLoopGroup ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast("decoder", new HttpRequestDecoder())
                                    .addLast("encoder", new HttpResponseEncoder())
                                    .addLast("handler", new NettyHttpServerHandler(getHttpHandler(), httpServer));
                        }
                    });

            bindPort();
        } catch (Throwable e) {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            throw e;
        }
    }

    @Override
    protected void doBind(InetSocketAddress localAddress) {
        ChannelFuture channelFuture = bootstrap.bind(localAddress);
        channelFuture.syncUninterruptibly();
        serverChannel = (ServerChannel) channelFuture.channel();
    }

    @Override
    public void doClose() {
        try {
            if (serverChannel != null) {
                // unbind.
                serverChannel.close();
            }
        } catch (Throwable e) {
            logger.warn("Http ServerChannel close failed", e);
        }

        Set<Channel> channels = getConnectedChannels();
        for (Channel channel : channels) {
            try {
                channel.close();
            } catch (Throwable e) {
                logger.warn("Http connected channel close failed", e);
            }
        }

        try {
            if (bootstrap != null) {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
                bootstrap = null;
            }
        } catch (Throwable e) {
            logger.warn("Http NioWorkerGroup shutdown failed", e);
        }
        try {
            if (channels != null) {
                channels.clear();
            }
        } catch (Throwable e) {
            logger.warn("Http clear channels failed", e);
        }
        httpServer = null;
    }

    private Set<Channel> getConnectedChannels() {
        Set<Channel> chs = new HashSet<Channel>();
        for (Channel channel : this.channels.values()) {
            if (channel.isActive()) {
                chs.add(channel);
            } else {
                channels.remove(NetUtil.toIpPort((InetSocketAddress) channel.remoteAddress()));
            }
        }
        return chs;
    }
}
