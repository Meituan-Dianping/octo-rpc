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
package com.meituan.dorado.serialize.thrift;

import com.meituan.dorado.bootstrap.invoker.ServiceInvocationRepository;
import com.meituan.dorado.bootstrap.provider.ServicePublisher;
import com.meituan.dorado.codec.octo.MetaUtil;
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.exception.ProtocolException;
import com.meituan.dorado.common.exception.TimeoutException;
import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.rpc.meta.RpcResult;
import com.meituan.dorado.trace.meta.TraceTimeline;
import com.meituan.dorado.transport.meta.DefaultRequest;
import com.meituan.dorado.transport.meta.DefaultResponse;
import com.meituan.dorado.util.BytesUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ThriftCodecSerializer {

    private static ThriftIDLSerializer idlSerializer = new ThriftIDLSerializer();
    private static ThriftAnnotationSerializer annotationSerializer = new ThriftAnnotationSerializer();
    private static ConcurrentMap<Class<?>, ThriftType> invocationSerializer = new ConcurrentHashMap<>();

    enum ThriftType {
        IDL, ANNOTATION
    }

    public static RpcInvocation decodeReqBody(byte[] buff, DefaultRequest request) throws Exception {
        Class<?> iface = ServicePublisher.getInterface(request.getServiceName());
        if (iface == null) {
            throw new ProtocolException("No match interface of service=" + request.getServiceName());
        }
        request.setServiceInterface(iface);
        ThriftMessageSerializer serializer = getSerializer(iface);
        return (RpcInvocation) serializer.deserialize4OctoThrift(buff, request);
    }

    public static RpcResult decodeRspBody(byte[] buff, DefaultResponse response) throws Exception {
        DefaultRequest request = (DefaultRequest) ServiceInvocationRepository.getRequest(response.getSeq());
        if (request == null) {
            throw new TimeoutException("Request has removed, cause Timeout happened before.");
        }

        Class<?> iface = request.getServiceInterface();
        if (iface == null) {
            throw new ProtocolException("Thrift decode responseBody: interface is null.");
        }
        response.setServiceInterface(iface);

        ThriftMessageSerializer serializer = getSerializer(iface);
        return (RpcResult) serializer.deserialize4OctoThrift(buff, response);
    }

    public static byte[] encodeReqBody(DefaultRequest request) throws Exception {
        Class<?> iface = request.getServiceInterface();
        if (iface == null) {
            throw new ProtocolException("Thrift encode requestBody: interface is null.");
        }

        ThriftMessageSerializer serializer = getSerializer(iface);
        return serializer.serialize(request);
    }

    public static byte[] encodeRspBody(DefaultResponse response) throws Exception {
        Class<?> iface = response.getServiceInterface();
        if (iface == null) {
            throw new ProtocolException("Thrift encode responseBody: interface is null.");
        }

        ThriftMessageSerializer serializer = getSerializer(iface);
        return serializer.serialize(response);
    }

    public static Object decodeThrift(byte[] buff, Map<String, Object> attachments) throws Exception {
        Class<?> iface = (Class<?>) attachments.get(Constants.SERVICE_IFACE);
        if (iface == null) {
            throw new ProtocolException("Origin thrift just support one service per port.");
        }
        ThriftMessageSerializer serializer = getSerializer(iface);
        return serializer.deserialize4Thrift(buff, iface, attachments);
    }

    public static byte[] encodeThrift(Object obj) throws Exception {
        byte[] bodyBytes;
        ThriftMessageSerializer serializer;
        if (obj instanceof DefaultRequest) {
            DefaultRequest request = (DefaultRequest) obj;
            TraceTimeline.record(TraceTimeline.ENCODE_START_TS, request);
            serializer = getSerializer(request.getServiceInterface());
        } else if (obj instanceof DefaultResponse) {
            DefaultResponse response = (DefaultResponse) obj;
            TraceTimeline.record(TraceTimeline.ENCODE_START_TS, response.getRequest());
            serializer = getSerializer(response.getServiceInterface());
        } else {
            throw new ProtocolException("Thrift encode object type is invalid.");
        }
        bodyBytes = serializer.serialize(obj);
        int bodyLen = bodyBytes.length;
        byte[] wholeMsgBuff = new byte[4 + bodyLen];
        BytesUtil.int2bytes(bodyLen, wholeMsgBuff, 0);
        System.arraycopy(bodyBytes, 0, wholeMsgBuff, 4, bodyLen);
        MetaUtil.recordTraceInfoInEncode(wholeMsgBuff, obj);
        return wholeMsgBuff;
    }

    protected static ThriftMessageSerializer getSerializer(Class<?> clazz) {
        ThriftType thriftType = invocationSerializer.get(clazz);

        if (thriftType == null) {
            if (ThriftUtil.isIDL(clazz)) {
                thriftType = ThriftType.IDL;
                invocationSerializer.putIfAbsent(clazz, thriftType);
            } else if (ThriftUtil.isAnnotation(clazz)) {
                thriftType = ThriftType.ANNOTATION;
                invocationSerializer.putIfAbsent(clazz, thriftType);
            } else {
                throw new ProtocolException("Thrift get serializer by interface failed: " + clazz.getName() +
                        " do not support thrift serialize");
            }
        }
        switch (thriftType) {
            case IDL:
                return idlSerializer;
            case ANNOTATION:
                return annotationSerializer;
            default:
                throw new ProtocolException("Thrift get serializer by interface failed: " + clazz.getName() +
                        " not find match serializer");
        }
    }
}
