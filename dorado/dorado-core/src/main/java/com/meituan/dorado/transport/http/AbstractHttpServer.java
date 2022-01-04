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
package com.meituan.dorado.transport.http;

import com.meituan.dorado.check.http.HttpCheckHandler;
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.common.extension.ExtensionLoader;
import com.meituan.dorado.common.util.NetUtil;
import com.meituan.dorado.rpc.handler.http.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Random;

public abstract class AbstractHttpServer implements HttpServer {

    private static final Logger logger = LoggerFactory.getLogger(AbstractHttpServer.class);

    private volatile boolean started;
    private volatile boolean destroyed;
    private static final int PORT_BIND_RETRY_TIMES = 10;

    protected HttpHandler httpHandler;
    protected InetSocketAddress localAddress;
    private volatile ShutDownHook hook;

    private static Random random = new Random();

    public AbstractHttpServer(RpcRole role) {
        this.httpHandler = ExtensionLoader.getExtension(HttpCheckHandler.class);
        httpHandler.setRole(role);
        start();
    }

    @Override
    public void close() {
        synchronized (HttpServer.class) {
            if (destroyed) {
                return;
            }
            logger.info("Closing HttpServer bind {}", localAddress);
            try {
                doClose();
            } catch (Throwable e) {
                logger.error("HttpServer close failed", e);
            }
            started = false;
            destroyed = true;
        }
    }

    @Override
    public boolean isStart() {
        return started;
    }

    @Override
    public HttpHandler getHttpHandler() {
        return httpHandler;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    public void start() {
        synchronized (HttpServer.class) {
            if (started) {
                return;
            }
            try {
                doStart();
                started = true;
                logger.info("Start HttpServer bind {}", localAddress);
            } catch (Throwable e) {
                // http是服务功能不影响核心链路，不抛异常只输出日志
                logger.error("HttpServer start failed", e);
            }
        }
        addShutDownHook();
    }

    private synchronized void addShutDownHook() {
        if (hook == null) {
            hook = new ShutDownHook(this);
            Runtime.getRuntime().addShutdownHook(hook);
        }
    }

    protected void bindPort() {
        int port = NetUtil.getAvailablePort(Constants.DEFAULT_HTTP_SERVER_PORT);
        int bindCount = 0;
        while (!started) {
            try {
                bindCount++;
                if (bindCount > 1) {
                    // 测试该重试策略, 50个进程并发没有问题
                    try {
                        // 随机等待 减少并发冲突
                        Thread.sleep(random.nextInt(100));
                    } catch (InterruptedException e) {
                    }
                    if (bindCount <= PORT_BIND_RETRY_TIMES / 2) {
                        port = NetUtil.getAvailablePort(port++);
                    } else {
                        // 重试五次后都失败, 则端口尝试增加随机值避免冲突
                        port = NetUtil.getAvailablePort(port + random.nextInt(10));
                    }
                }
                localAddress = new InetSocketAddress(port);
                doBind(localAddress);
                break;
            } catch (Throwable e) {
                if (bindCount > PORT_BIND_RETRY_TIMES) {
                    throw e;
                }
                logger.info("HttpServer bind {} failed, will do {} retry, errorMsg: {}", localAddress, bindCount, e.getMessage());
            }
        }
    }

    protected abstract void doStart();

    protected abstract void doBind(InetSocketAddress localAddress);

    protected abstract void doClose();

    class ShutDownHook extends Thread {
        private HttpServer server;

        public ShutDownHook(HttpServer server) {
            this.server = server;
        }

        @Override
        public void run() {
            hook = null;
            server.close();
        }
    }
}
