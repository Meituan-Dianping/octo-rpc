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
package com.meituan.dorado.mock;


import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.rpc.handler.http.HttpHandler;
import com.meituan.dorado.transport.http.HttpServer;
import com.meituan.dorado.transport.http.HttpServerFactory;

import java.net.InetSocketAddress;

public class MockHttpServerFactory implements HttpServerFactory {

    @Override
    public HttpServer buildServer(RpcRole rpcRole) {
        return new MockHttpServer();
    }

    public static class MockHttpServer implements HttpServer {

        @Override
        public InetSocketAddress getLocalAddress() {
            return null;
        }

        @Override
        public HttpHandler getHttpHandler() {
            return null;
        }

        @Override
        public void close() {
        }

        @Override
        public boolean isStart() {
            return true;
        }
    }
}
