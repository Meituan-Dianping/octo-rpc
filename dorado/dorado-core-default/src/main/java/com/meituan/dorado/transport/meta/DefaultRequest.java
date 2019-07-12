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

import com.meituan.dorado.codec.octo.meta.MessageType;
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.serialize.thrift.ThriftMessageSerializer.ThriftMessageInfo;
import com.meituan.dorado.trace.meta.TransportTraceInfo;
import com.meituan.dorado.transport.Client;
import com.meituan.dorado.util.CompressUtil.CompressType;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultRequest implements Request {

    // thrift seqid是int，协议时为复用该字段此处使用Integer
    private static final AtomicInteger INVOKER_SEQ = new AtomicInteger(0);

    private final long startTimestamp = System.currentTimeMillis();

    private Long seq;

    private String appkey;

    private String remoteAppkey;

    private String serviceName;

    private Class<?> serviceInterface;

    private Boolean isHeartbeat;

    private RpcInvocation data;

    private byte version;

    private int timeout;

    private int messageType = MessageType.Normal.getValue();

    private String protocol;

    private byte serialize;

    private int callType; // 0: replay; 1: noReplay

    private String clientIp; // 协议中传递的调用端IP

    private CompressType compressType = CompressType.NO;

    private Boolean doChecksum = false;

    private Boolean octoProtocol = false;

    private ThriftMessageInfo thriftMsgInfo;

    private TransportTraceInfo transportTraceInfo; // 调用端传向服务端的traceInfo

    private InetSocketAddress remoteAddress;

    // 请求处理的相关上下文消息
    private Map<String, Object> attachments;

    // localContexts和globalContexts的参数将作为协议内容传递给对端
    private Map<String, String> globalContexts = null;
    private Map<String, String> localContexts = null;

    // 传输层赋值, 无需设置, 作用于Server处理请求时判断请求服务是否是当前端口可提供的服务
    private Object serviceImpl;

    private Client client;

    public DefaultRequest() {
        seq = Long.valueOf(INVOKER_SEQ.getAndIncrement());
    }

    public DefaultRequest(Long seqId) {
        seq = seqId;
    }

    @Override
    public Long getSeq() {
        return seq;
    }

    public Integer getSeqToInt() {
        return seq.intValue();
    }

    public void setSeq(Long seq) {
        this.seq = seq;
    }

    public String getAppkey() {
        return appkey;
    }

    public void setAppkey(String appkey) {
        this.appkey = appkey;
    }

    public String getRemoteAppkey() {
        return remoteAppkey;
    }

    public void setRemoteAppkey(String remoteAppkey) {
        this.remoteAppkey = remoteAppkey;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Class<?> getServiceInterface() {
        return serviceInterface;
    }

    public void setServiceInterface(Class<?> serviceInterface) {
        this.serviceInterface = serviceInterface;
    }

    @Override
    public RpcInvocation getData() {
        return data;
    }

    @Override
    public void setData(RpcInvocation data) {
        this.data = data;
    }

    @Override
    public Boolean isHeartbeat() {
        return isHeartbeat;
    }

    public void setHeartbeat(Boolean heartbeat) {
        isHeartbeat = heartbeat;
    }

    @Override
    public byte getVersion() {
        return version;
    }

    public void setVersion(byte version) {
        this.version = version;
    }

    @Override
    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @Override
    public byte getSerialize() {
        return serialize;
    }

    public void setSerialize(byte serialize) {
        this.serialize = serialize;
    }

    @Override
    public int getCallType() {
        return callType;
    }

    public void setCallType(int callType) {
        this.callType = callType;
    }

    @Override
    public int getTimeout() {
        return timeout;
    }

    @Override
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public CompressType getCompressType() {
        return compressType;
    }

    public void setCompressType(CompressType compressType) {
        this.compressType = compressType;
    }

    public Boolean getDoChecksum() {
        return doChecksum;
    }

    public void setDoChecksum(Boolean doChecksum) {
        this.doChecksum = doChecksum;
    }

    public Boolean isOctoProtocol() {
        return octoProtocol;
    }

    public void setOctoProtocol(Boolean octoProtocol) {
        this.octoProtocol = octoProtocol;
    }

    @Override
    public Object getAttachment(String key) {
        if (attachments == null) {
            return null;
        }
        return attachments.get(key);
    }

    @Override
    public void putAttachment(String key, Object value) {
        if (this.attachments == null) {
            this.attachments = new HashMap<>();
        }
        this.attachments.put(key, value);
    }

    public Map<String, Object> getAttachments() {
        return attachments;
    }

    public void putAttachments(Map<String, Object> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        if (this.attachments == null) {
            this.attachments = attachments;
        } else {
            this.attachments.putAll(attachments);
        }
    }

    public Map<String, String> getGlobalContexts() {
        return globalContexts;
    }

    public void setGlobalContext(Map<String, String> globalContexts) {
        this.globalContexts = globalContexts;
    }

    @Override
    public String getLocalContext(String key) {
        if (localContexts == null) {
            return null;
        }
        return localContexts.get(key);
    }

    @Override
    public void putLocalContext(String key, String value) {
        if (this.localContexts == null) {
            this.localContexts = new HashMap<>();
        }
        this.localContexts.put(key, value);
    }

    public Map<String, String> getLocalContexts() {
        return localContexts;
    }

    public void putLocalContexts(Map<String, String> localContexts) {
        if (localContexts == null || localContexts.isEmpty()) {
            return;
        }
        if (this.localContexts == null) {
            this.localContexts = localContexts;
        } else {
            this.localContexts.putAll(localContexts);
        }
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public void setRemoteAddress(InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    @Override
    public Object getServiceImpl() {
        return serviceImpl;
    }

    @Override
    public void setServiceImpl(Object serviceImpl) {
        this.serviceImpl = serviceImpl;
    }

    @Override
    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public ThriftMessageInfo getThriftMsgInfo() {
        return thriftMsgInfo;
    }

    public void setThriftMsgInfo(ThriftMessageInfo thriftMsgInfo) {
        this.thriftMsgInfo = thriftMsgInfo;
    }

    public TransportTraceInfo getTransportTraceInfo() {
        return transportTraceInfo;
    }

    public void setTransportTraceInfo(TransportTraceInfo transportTraceInfo) {
        this.transportTraceInfo = transportTraceInfo;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    @Override
    public String toString() {
        return "Request(" + serviceName + ")";
    }
}
