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
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.exception.TransportException;
import com.meituan.dorado.common.extension.ExtensionLoader;
import com.meituan.dorado.common.thread.DefaultThreadFactory;
import com.meituan.dorado.config.service.ClientConfig;
import com.meituan.dorado.degrade.NodeDegrade;
import com.meituan.dorado.rpc.DefaultFuture;
import com.meituan.dorado.rpc.ResponseFuture;
import com.meituan.dorado.rpc.handler.invoker.Invoker;
import com.meituan.dorado.transport.meta.Request;
import com.meituan.dorado.transport.support.InvokerChannelHandler;
import com.meituan.dorado.util.ClazzUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractClient implements Client {

    private static final Logger logger = LoggerFactory.getLogger(AbstractClient.class);

    private static final ScheduledThreadPoolExecutor reconnectExecutor = new ScheduledThreadPoolExecutor(1,
            new DefaultThreadFactory("DoradoClientReconnectTimer", true));

    protected final ClientConfig clientConfig;
    private final ChannelHandler handler;
    private final InetSocketAddress remoteAddress;
    private final Codec codec;
    private final Invoker invoker;

    private volatile boolean closed;
    private final ReentrantLock connectLock = new ReentrantLock();
    private ScheduledFuture reconnectExecutorFuture = null;
    private AtomicBoolean isDegrade = new AtomicBoolean(false);

    public AbstractClient(ClientConfig config, Invoker invoker) {
        this.remoteAddress = new InetSocketAddress(config.getAddress().getIp(), config.getAddress().getPort());
        this.clientConfig = config;
        this.handler = new InvokerChannelHandler();
        this.codec = ExtensionLoader.getExtension(Codec.class);
        this.invoker = invoker;

        try {
            init();
        } catch (Throwable e) {
            close();
            throw new TransportException("Failed to start " + getClass().getName()
                    + " connect to server " + remoteAddress, e);
        }

        try {
            connect();
        } catch (TransportException e) {
            if (e.getCause() != null && e.getCause() instanceof ConnectException) {
                close();
                throw e;
            }
            // 连接失败 提示warning, 权重置为0, 重连
            String message = "Failed to start " + ClazzUtil.getClazzSimpleName(getClass()) + " connect to the server "
                    + getRemoteAddress() + ". Will retry later. Cause: " + e.getMessage();
            logger.warn(message, e);
            NodeDegrade.weightDegradeToZero(invoker, message);
            isDegrade.set(true);
            connectStatusCheck();
        } catch (Throwable e) {
            close();
            throw new TransportException(
                    "Failed to start " + ClazzUtil.getClazzSimpleName(getClass()) + " connect to the server "
                            + getRemoteAddress(), e);
        }
    }

    @Override
    public ResponseFuture request(Object message, int timeout) {
        if (isClosed()) {
            throw new TransportException("Failed to send message " + message + ", cause: The client " + this + " is closed!");
        }

        Request req;
        if (message instanceof Request) {
            req = ((Request) message);
        } else {
            throw new IllegalArgumentException("Message must be instance of request, message:" + message);
        }

        DefaultFuture future = (DefaultFuture) req.getAttachment(Constants.RESPONSE_FUTURE);
        if (future == null) {
            throw new IllegalArgumentException("Message's responseFuture equals null, message:" + message);
        }
        future.setTimeout(timeout);

        try {
            if (!isConnected()) {
                connect();
            }
            Channel channel = getChannel();
            future.setChannel(channel);
            handler.send(channel, message);
        } catch (Exception e) {
            connectStatusCheck();
            future.cancel();
            throw e;
        }
        return future;
    }

    @Override
    public ResponseFuture request(Object message) {
        return request(message, getTimeout());
    }

    public void connect() {
        connectLock.lock();
        try {
            if (isConnected()) {
                return;
            }
            doConnect();
            if (!isConnected()) {
                throw new TransportException("Failed connect to server " + getRemoteAddress() + " from " + getClass().getName()
                        + ", cause: Connect wait timeout: " + getConnTimeout() + "ms.");
            } else {
                logger.info(
                        "Succeed connect to server " + getRemoteAddress() + " from " + getClass().getName() + ", channel is " + this.getChannel());

            }
        } catch (TransportException e) {
            throw e;
        } finally {
            connectLock.unlock();
        }
    }

    public void disconnect() {
        connectLock.lock();
        try {
            logger.info("Disconnecting to server {}", remoteAddress);
            stopConnectStatusCheck();
            Channel channel = getChannel();
            if (channel != null) {
                channel.close();
            }
        } catch (Throwable e) {
            logger.warn("Disconnect to server {} failed", remoteAddress, e);
        } finally {
            connectLock.unlock();
        }
    }

    @Override
    public void reconnect() {
        logger.info("Client[connect to {}] do reconnect", getRemoteAddress());
        try {
            disconnect();
            connect();
        } catch (Throwable e) {
            logger.error("Client[connect to {}] reconnect failed", getRemoteAddress());
            throw e;
        }
    }

    @Override
    public void close() {
        logger.info("Closing {} connect to server {}", getClass().getName(), remoteAddress);
        closed = true;
        try {
            handler.destroy();
            disconnect();
            doClose();
        } catch (Throwable e) {
            logger.error("Close {} connect to server {} failed", getClass().getName(), remoteAddress, e);
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public boolean isConnected() {
        return getChannel() != null && getChannel().isConnected();
    }

    @Override
    public Codec getCodec() {
        return codec;
    }

    @Override
    public ChannelHandler getChannelHandler() {
        return handler;
    }

    private void connectStatusCheck() {
        if (reconnectExecutorFuture == null || reconnectExecutorFuture.isCancelled()) {
            Runnable connectStatusCheckTask = new Runnable() {
                @Override
                public void run() {
                    try {
                        doConnect();
                        if (isDegrade.get()) {
                            NodeDegrade.weightRecover(invoker);
                            isDegrade.set(false);
                        }
                        stopConnectStatusCheck();
                    } catch (Throwable t) {
                        String message = "Failed to connect to the server "
                                + getRemoteAddress() + ". Will retry later. Cause: " + t.getMessage();
                        logger.warn(message);
                        if (!isDegrade.get()) {
                            // 降权
                            NodeDegrade.weightDegradeToZero(invoker, message);
                            isDegrade.set(true);
                        }
                    }
                }
            };
            reconnectExecutorFuture = reconnectExecutor.scheduleWithFixedDelay(connectStatusCheckTask,
                    0, Constants.DEFAULT_RECONN_INTERVAL, TimeUnit.MILLISECONDS);
            logger.info("Start check {} connection status", getRemoteAddress());
        }

    }

    private void stopConnectStatusCheck() {
        try {
            if (reconnectExecutorFuture != null && !reconnectExecutorFuture.isDone()) {
                reconnectExecutorFuture.cancel(true);
                reconnectExecutor.purge();
                logger.info("Stop check {} connection status", getRemoteAddress());
            }
        } catch (Throwable e) {
            logger.warn("StopConnectStatusCheck failed", e);
        }
    }

    protected abstract void init();

    protected abstract void doClose();

    protected abstract int getConnTimeout();

    protected abstract int getTimeout();

    protected abstract void doConnect();
}