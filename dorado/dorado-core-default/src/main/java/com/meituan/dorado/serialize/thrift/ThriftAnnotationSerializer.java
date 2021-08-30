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


import com.facebook.swift.codec.internal.TProtocolWriter;
import com.meituan.dorado.bootstrap.invoker.ServiceInvocationRepository;
import com.meituan.dorado.codec.octo.MetaUtil;
import com.meituan.dorado.codec.octo.meta.old.RequestHeader;
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.exception.ProtocolException;
import com.meituan.dorado.rpc.GenericService;
import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.rpc.meta.RpcResult;
import com.meituan.dorado.serialize.thrift.annotation.ThriftAnnotationManager;
import com.meituan.dorado.serialize.thrift.annotation.codec.ThriftMethodCodec;
import com.meituan.dorado.serialize.thrift.annotation.codec.ThriftServiceCodec;
import com.meituan.dorado.transport.meta.DefaultRequest;
import com.meituan.dorado.transport.meta.DefaultResponse;
import com.meituan.dorado.transport.meta.Request;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.transport.TMemoryBuffer;
import org.apache.thrift.transport.TMemoryInputTransport;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ThriftAnnotationSerializer extends ThriftMessageSerializer {

    @Override
    protected byte[] serialize(Object obj) throws Exception {
        TMemoryBuffer memoryBuffer = new TMemoryBuffer(INITIAL_BYTE_ARRAY_SIZE);
        TBinaryProtocol protocol = new TBinaryProtocol(memoryBuffer);

        if (obj instanceof DefaultRequest) {
            DefaultRequest request = (DefaultRequest) obj;
            return doSerializeRequest(request, protocol, memoryBuffer);
        } else if (obj instanceof DefaultResponse) {
            DefaultResponse response = (DefaultResponse) obj;
            return doSerializeResponse(response, protocol, memoryBuffer);
        } else {
            throw new ProtocolException("Object type is invalid when serialize with octo protocol");
        }
    }

    @Override
    protected Object deserialize4OctoThrift(byte[] buff, Object obj) throws Exception {
        TMemoryInputTransport transport = new TMemoryInputTransport(buff);
        TBinaryProtocol protocol = new TBinaryProtocol(transport);

        if (obj instanceof DefaultRequest) {
            DefaultRequest request = (DefaultRequest) obj;
            TMessage message = protocol.readMessageBegin();
            if (message.type != TMessageType.CALL) {
                throw new ProtocolException("Thrift deserialize request: message type is invalid.");
            }
            return doDeserializeRequest(request, protocol, message);
        } else if (obj instanceof DefaultResponse) {
            DefaultResponse response = (DefaultResponse) obj;
            return doDeserializeResponse(response, protocol);
        } else {
            throw new ProtocolException("Object type is invalid when deserialize with octo protocol");
        }
    }

    @Override
    protected Object deserialize4Thrift(byte[] buff, Class<?> iface, Map<String, Object> attachments) throws Exception {
        TMemoryInputTransport transport = new TMemoryInputTransport(buff);
        TBinaryProtocol protocol = new TBinaryProtocol(transport);
        TMessage message = protocol.readMessageBegin();
        if (message.type == TMessageType.CALL) {
            DefaultRequest request = new DefaultRequest(Long.valueOf(message.seqid));
            request.setServiceInterface(iface);
            RpcInvocation rpcInvocation = doDeserializeRequest(request, protocol, message);
            request.setData(rpcInvocation);
            return request;
        } else if (message.type == TMessageType.REPLY || message.type == TMessageType.EXCEPTION) {
            DefaultResponse response = new DefaultResponse(Long.valueOf(message.seqid));
            response.setServiceInterface(iface);
            try {
                RpcResult rpcResult = doDeserializeResponse(response, protocol);
                response.setResult(rpcResult);
            } catch (Exception e) {
                response.setException(e);
            }
            return response;
        } else {
            throw new ProtocolException("Thrift deserialize message type is invalid");
        }
    }

    private RpcInvocation doDeserializeRequest(DefaultRequest request, TBinaryProtocol protocol, TMessage message) throws Exception {
        Class<?> serviceInterface = request.getServiceInterface();
        if (request.getLocalContext(Constants.GENERIC_KEY) != null) {
            serviceInterface = GenericService.class;
        }

        ThriftMethodCodec methodCodec = null;

        ThriftServiceCodec serverCodec = ThriftAnnotationManager.getServerCodec(serviceInterface);
        if (serverCodec != null) {
            methodCodec = serverCodec.getMethodCodecByValue(message.name);
        }
        if (methodCodec == null) {
            throw new ProtocolException("Thrift deserialize request: no valid methodProcessor.");
        }

        ThriftMessageInfo thriftMessageInfo = new ThriftMessageInfo(message.name, message.seqid);
        if (!request.isOctoProtocol()) {
            if (hasOldRequestHeader(protocol)) {
                // 解析老协议的header
                RequestHeader requestHeader = new RequestHeader();
                protocol.readFieldBegin();
                requestHeader.read(protocol);
                protocol.readFieldEnd();
                request = MetaUtil.convertOldProtocolHeaderToRequest(requestHeader, request);
                thriftMessageInfo.setOldProtocol(true);
            }
            request.setServiceName(request.getServiceInterface().getName());
        }
        request.setThriftMsgInfo(thriftMessageInfo);

        List<Class<?>> parameterTypes = new ArrayList<>();
        Object[] args = methodCodec.readArguments(protocol, parameterTypes);

        protocol.readMessageEnd();

        Class<?>[] parameterTypeArray = parameterTypes.toArray(new Class[parameterTypes.size()]);
        Method method = methodCodec.getMethod();
        RpcInvocation invocation = new RpcInvocation(request.getServiceInterface(), method, args, parameterTypeArray);
        return invocation;
    }

    private RpcResult doDeserializeResponse(DefaultResponse response, TBinaryProtocol protocol) throws Exception {
        Class<?> serviceInterface = response.getServiceInterface();
        Request request = ServiceInvocationRepository.getRequest(response.getSeq());
        if (request == null) {
            return null;
        }

        Method method = request.getData().getMethod();
        ThriftServiceCodec clientCodec = ThriftAnnotationManager.getClientCodec(serviceInterface);
        if (clientCodec == null) {
            return null;
        }
        ThriftMethodCodec methodCodec = clientCodec.getMethodCodecByName(method.getName());

        Object result = null;
        try {
            result = methodCodec.readResponse(protocol, response.getSeqToInt());
        } catch (Exception e) {
            response.setException(e);
        }
        if (result instanceof Exception) {
            // 服务端自定义异常
            response.setException((Exception) result);
        }
        RpcResult rpcResult = new RpcResult();
        rpcResult.setReturnVal(result);

        if (!response.isOctoProtocol() && hasOldRequestHeader(protocol)) {
            // 解析老协议的Header
            RequestHeader requestHeader = new RequestHeader();
            protocol.readFieldBegin();
            requestHeader.read(protocol);
            protocol.readFieldEnd();
        }
        protocol.readMessageEnd();
        return rpcResult;
    }

    private byte[] doSerializeRequest(DefaultRequest request, TBinaryProtocol protocol, TMemoryBuffer memoryBuffer) throws Exception {
        RpcInvocation invocation = request.getData();
        Object[] args = invocation.getArguments();
        Class<?> serviceInterface = request.getServiceInterface();
        ThriftServiceCodec clientCodec = ThriftAnnotationManager.getClientCodec(serviceInterface);
        if (clientCodec == null) {
            return null;
        }
        ThriftMethodCodec methodCodec = clientCodec.getMethodCodecByName(invocation.getMethod().getName());

        // 优先获取thriftMethod定义的alias
        String methodName = methodCodec.getMethodAliasName();
        TMessage message = new TMessage(methodName, TMessageType.CALL, request.getSeqToInt());
        protocol.writeMessageBegin(message);

        TProtocolWriter writer = new TProtocolWriter(protocol);
        writer.writeStructBegin(methodName + "_args");
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                Object value = args[i];
                ThriftMethodCodec.ParameterCodec parameter = methodCodec.getClientParameterCodec(i);
                writer.writeField(parameter.getName(), parameter.getId(), parameter.getCodec(), value);
            }
        }
        writer.writeStructEnd();

        protocol.writeMessageEnd();
        protocol.getTransport().flush();
        return getActualBytes(memoryBuffer);
    }

    private byte[] doSerializeResponse(DefaultResponse response, TBinaryProtocol protocol, TMemoryBuffer memoryBuffer) throws Exception {
        ThriftMessageInfo thriftMsgInfo = response.getThriftMsgInfo();
        if (thriftMsgInfo == null) {
            throw new ProtocolException("Thrift serialize response: no thrift message info");
        }

        Class<?> serviceInterface = response.getServiceInterface();
        if (response.getLocalContext(Constants.GENERIC_KEY) != null) {
            serviceInterface = GenericService.class;
        }

        ThriftServiceCodec serverCodec = ThriftAnnotationManager.getServerCodec(serviceInterface);
        ThriftMethodCodec methodCodec = null;
        if (serverCodec != null) {
            methodCodec = serverCodec.getMethodCodecByValue(thriftMsgInfo.methodName);
        }
        if (methodCodec == null) {
            throw new ProtocolException("Thrift deserialize request: no valid methodProcessor.");
        }

        if (response.getException() == null) {
            methodCodec.writeResponse(protocol, thriftMsgInfo.seqId, TMessageType.REPLY, "success",
                    (short) 0, response.getResult().getReturnVal());
        } else {
            methodCodec.writeResponse(protocol, thriftMsgInfo.seqId, TMessageType.EXCEPTION, "exception",
                    (short) 0, response.getException());
        }
        return getActualBytes(memoryBuffer);
    }
}