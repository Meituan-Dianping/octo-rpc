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

import com.meituan.dorado.bootstrap.invoker.ServiceInvocationRepository;
import com.meituan.dorado.common.exception.TimeoutException;
import com.meituan.dorado.rpc.meta.RpcResult;
import com.meituan.dorado.serialize.thrift.ThriftMessageSerializer.ThriftMessageInfo;
import com.meituan.dorado.util.CompressUtil;

import java.util.HashMap;
import java.util.Map;

public class DefaultResponse implements Response {

    private Long seq;

    private String serviceName;

    private Class<?> serviceInterface;

    private byte statusCode;

    private RpcResult result;

    private Throwable exception;

    private Boolean heartbeat;

    private int messageType;

    private byte serialize;

    private byte version;

    private Request request;

    private CompressUtil.CompressType compressType;

    private Boolean doChecksum;

    private Boolean octoProtocol = false;

    private int port;

    private ThriftMessageInfo thriftMsgInfo;

    private String protocol;

    private Map<String, Object> attachments;

    private Map<String, String> globalContexts = null;
    private Map<String, String> localContexts = null;

    public DefaultResponse(Long seqId) {
        this.seq = seqId;
        DefaultRequest request = (DefaultRequest) ServiceInvocationRepository.getRequest(seqId);
        initField(request);
    }

    public DefaultResponse(DefaultRequest request) {
       initField(request);
    }

    private void initField(DefaultRequest request) {
        if (request == null) {
            throw new TimeoutException("Request has removed, cause Timeout happened earlier.");
        }
        this.seq = request.getSeq();
        this.serviceName = request.getServiceName();
        this.serviceInterface = request.getServiceInterface();
        this.messageType = request.getMessageType();
        this.doChecksum = request.getDoChecksum();
        this.compressType = request.getCompressType();
        this.serialize = request.getSerialize();
        this.octoProtocol = request.isOctoProtocol();
        this.thriftMsgInfo = request.getThriftMsgInfo();
        this.protocol = request.getProtocol();
        this.request = request;
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

    public Class<?> getServiceInterface() {
        return serviceInterface;
    }

    public void setServiceInterface(Class<?> serviceInterface) {
        this.serviceInterface = serviceInterface;
    }

    @Override
    public byte getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(byte statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public RpcResult getResult() {
        return result;
    }

    @Override
    public void setResult(RpcResult result) {
        this.result = result;
    }

    @Override
    public Boolean isHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(Boolean heartbeat) {
        this.heartbeat = heartbeat;
    }

    @Override
    public Throwable getException() {
        return exception;
    }

    @Override
    public void setException(Throwable exception) {
        this.exception = exception;
    }

    @Override
    public byte getVersion() {
        return version;
    }

    @Override
    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
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

    public CompressUtil.CompressType getCompressType() {
        return compressType;
    }

    public void setCompressType(CompressUtil.CompressType compressType) {
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

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
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

    public void setGlobalContexts(Map<String, String> globalContext) {
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

    public ThriftMessageInfo getThriftMsgInfo() {
        return thriftMsgInfo;
    }

    public void setThriftMsgInfo(ThriftMessageInfo thriftMsgInfo) {
        this.thriftMsgInfo = thriftMsgInfo;
    }

    @Override
    public String toString() {
        return "Response(" + serviceName + ")";
    }
}
