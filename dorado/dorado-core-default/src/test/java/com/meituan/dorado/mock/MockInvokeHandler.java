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
import com.meituan.dorado.rpc.handler.InvokeHandler;
import com.meituan.dorado.rpc.meta.RpcResult;
import com.meituan.dorado.transport.meta.Request;
import com.meituan.dorado.transport.meta.Response;

public class MockInvokeHandler implements InvokeHandler {

    @Override
    public Response handle(Request requet) {
        return new MockResponse();
    }

    @Override
    public Response buildResponse(Request request) {
        return new MockResponse();
    }

    @Override
    public RpcRole getRole() {
        return RpcRole.MULTIROLE;
    }

    class MockResponse implements Response {

        @Override
        public Long getSeq() {
            return null;
        }

        @Override
        public byte getStatusCode() {
            return 0;
        }

        @Override
        public RpcResult getResult() {
            return null;
        }

        @Override
        public Throwable getException() {
            return null;
        }

        @Override
        public Boolean isHeartbeat() {
            return null;
        }

        @Override
        public int getMessageType() {
            return 0;
        }

        @Override
        public String getProtocol() {
            return null;
        }

        @Override
        public byte getSerialize() {
            return 0;
        }

        @Override
        public byte getVersion() {
            return 0;
        }

        @Override
        public Request getRequest() {
            return null;
        }

        @Override
        public void setResult(RpcResult rpcResult) {

        }

        @Override
        public Object getAttachment(String key) {
            return null;
        }

        @Override
        public void putAttachment(String key, Object value) {

        }

        @Override
        public void setException(Throwable e) {

        }

        @Override
        public String getLocalContext(String key) {
            return null;
        }

        @Override
        public void putLocalContext(String key, String value) {

        }
    }
}
