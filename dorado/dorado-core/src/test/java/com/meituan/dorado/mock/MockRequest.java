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


import com.meituan.dorado.MockUtil;
import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.transport.Client;
import com.meituan.dorado.transport.meta.Request;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class MockRequest implements Request {

    private static final AtomicLong INVOKER_SEQ = new AtomicLong(0);

    private Long seq;
    private RpcInvocation data;
    private Client client;

    public MockRequest() {
        this.seq = INVOKER_SEQ.getAndIncrement();
        try {
            this.data = MockUtil.getRpcInvocationWithoutRequest();
            this.client = MockUtil.getClient();
        } catch (Exception e) {
            //ignore
        }
    }

    @Override
    public Long getSeq() {
        return seq;
    }

    @Override
    public String getServiceName() {
        return null;
    }

    @Override
    public Object getServiceImpl() {
        return null;
    }

    @Override
    public void setServiceImpl(Object serviceImpl) {
    }

    @Override
    public RpcInvocation getData() {
        return data;
    }

    @Override
    public Client getClient() {
        return null;
    }

    @Override
    public Boolean isHeartbeat() {
        return null;
    }

    @Override
    public byte getVersion() {
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
    public int getMessageType() {
        return 0;
    }

    @Override
    public int getCallType() {
        return 0;
    }

    @Override
    public void setData(RpcInvocation data) {
        this.data = data;
    }

    @Override
    public void setTimeout(int timeout) {}

    @Override
    public Object getAttachment(String key) {
        return null;
    }

    @Override
    public void putAttachment(String key, Object value) {

    }

    @Override
    public String getLocalContext(String key) {
        return null;
    }

    @Override
    public void putLocalContext(String key, String value) {

    }
}
