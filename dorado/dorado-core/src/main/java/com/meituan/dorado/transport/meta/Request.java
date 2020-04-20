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
package com.meituan.dorado.transport.meta;

import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.transport.Client;

import java.net.InetSocketAddress;

/**
 * RPC invoke request
 */
public interface Request {

    Long getSeq();

    String getServiceName();

    Object getServiceImpl();

    void setServiceImpl(Object serviceImpl);

    RpcInvocation getData();

    Client getClient();

    Boolean isHeartbeat();

    byte getVersion();

    String getProtocol();

    byte getSerialize();

    int getMessageType();

    int getCallType();

    void setData(RpcInvocation data);

    void setTimeout(int timeout);

    int getTimeout();

    InetSocketAddress getRemoteAddress();

    void setRemoteAddress(InetSocketAddress remoteAddress);

    Object getAttachment(String key);

    void putAttachment(String key, Object value);

    String getLocalContext(String key);

    void putLocalContext(String key, String value);

}
