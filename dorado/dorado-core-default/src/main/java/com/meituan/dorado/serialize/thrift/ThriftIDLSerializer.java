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

import com.meituan.dorado.codec.octo.MetaUtil;
import com.meituan.dorado.codec.octo.meta.old.RequestHeader;
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.exception.ProtocolException;
import com.meituan.dorado.common.util.ClassLoaderUtil;
import com.meituan.dorado.common.util.CommonUtil;
import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.rpc.meta.RpcResult;
import com.meituan.dorado.trace.meta.TraceTimeline;
import com.meituan.dorado.transport.meta.DefaultRequest;
import com.meituan.dorado.transport.meta.DefaultResponse;
import com.meituan.dorado.util.MethodUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.apache.thrift.TFieldIdEnum;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TMemoryBuffer;
import org.apache.thrift.transport.TMemoryInputTransport;
import org.apache.thrift.transport.TTransport;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ThriftIDLSerializer extends ThriftMessageSerializer {
    private static final org.apache.thrift.protocol.TField MTRACE_FIELD_DESC = new org.apache.thrift.protocol.TField(
            "mtrace", org.apache.thrift.protocol.TType.STRUCT, (short) 32767);
    private static final String BYTE_ARRAY_CLASS_NAME = "[B";

    @Override
    protected Object deserialize4OctoThrift(byte[] buff, Object obj) throws Exception {
        TMemoryInputTransport transport = new TMemoryInputTransport(buff);
        TBinaryProtocol protocol = new TBinaryProtocol(transport);
        TMessage message = protocol.readMessageBegin();

        Object result;
        if (obj instanceof DefaultRequest) {
            result = doDeserializeRequest(buff, (DefaultRequest) obj, message, protocol);
        } else if (obj instanceof DefaultResponse) {
            result = doDeserializeResponse(buff, (DefaultResponse) obj, message, protocol);
        } else {
            throw new ProtocolException("Thrift octoProtocol object type is invalid, it should not happen.");
        }
        protocol.readMessageEnd();
        return result;
    }

    @Override
    protected Object deserialize4Thrift(byte[] buff, Class<?> iface, Map<String, Object> attachments) throws Exception {
        TraceTimeline timeline = TraceTimeline.newRecord(CommonUtil.objectToBool(attachments.get(Constants.TRACE_IS_RECORD_TIMELINE), false),
                TraceTimeline.DECODE_START_TS);

        Object obj = null;
        TMemoryInputTransport transport = new TMemoryInputTransport(buff);
        TBinaryProtocol protocol = new TBinaryProtocol(transport);
        TMessage message = protocol.readMessageBegin();
        if (message.type == TMessageType.CALL) {
            DefaultRequest request = new DefaultRequest(Long.valueOf(message.seqid));
            request.setServiceInterface(iface);
            RpcInvocation rpcInvocation = doDeserializeRequest(buff, request, message, protocol);
            request.setData(rpcInvocation);
            obj = request;

            rpcInvocation.putAttachment(Constants.TRACE_TIMELINE, timeline);
        } else if (message.type == TMessageType.REPLY || message.type == TMessageType.EXCEPTION) {
            DefaultResponse response = new DefaultResponse(Long.valueOf(message.seqid));
            response.setServiceInterface(iface);
            try {
                RpcResult rpcResult = doDeserializeResponse(buff, response, message, protocol);
                response.setResult(rpcResult);
            } catch (Exception e) {
                response.setException(e);
            }

            obj = response;
            if (response.getRequest() != null && response.getRequest().getData() != null) {
                TraceTimeline.copyRecord(timeline, response.getRequest().getData());
            }
        } else {
            throw new ProtocolException("Thrift deserialize message type is invalid");
        }
        protocol.readMessageEnd();

        MetaUtil.recordTraceInfoInDecode(buff, obj);
        return obj;
    }

    protected RpcInvocation doDeserializeRequest(byte[] buff, DefaultRequest request, TMessage message,
                                                 TProtocol protocol) throws Exception {
        if (message.type != TMessageType.CALL) {
            throw new ProtocolException("Thrift deserialize request: message type is invalid.");
        }

        if (!request.isOctoProtocol()) {
            if (hasOldRequestHeader(protocol)) {
                // 老协议的header
                RequestHeader requestHeader = new RequestHeader();
                protocol.readFieldBegin();
                requestHeader.read(protocol);
                protocol.readFieldEnd();
                request = MetaUtil.convertOldProtocolHeaderToRequest(requestHeader, request);
            }
            request.setServiceName(ThriftUtil.getIDLClassName(request.getServiceInterface()));
        }
        String argsClassName = ThriftUtil.generateArgsClassName(
                request.getServiceInterface().getName(), message.name);

        TBase args = getClazzInstance(argsClassName);
        try {
            args.read(protocol);
        } catch (TException e) {
            throw new ProtocolException("Thrift deserialize request failed.", e);
        }

        List<Object> arguments = new ArrayList<>();
        List<Class<?>> parameterTypes = new ArrayList<>();
        int index = 1;
        while (true) {
            TFieldIdEnum fieldIdEnum = args.fieldForId(index++);
            if (fieldIdEnum == null) {
                break;
            }

            String fieldName = fieldIdEnum.getFieldName();
            Class<?> clazz = cachedClass.get(argsClassName);
            Method getMethod = ThriftUtil.obtainGetMethod(clazz, fieldName);

            if (BYTE_ARRAY_CLASS_NAME.equals(getMethod.getReturnType().getName())) {
                parameterTypes.add(ByteBuffer.class);
                arguments.add(ByteBuffer.wrap((byte[]) args.getFieldValue(fieldIdEnum)));
            } else {
                parameterTypes.add(getMethod.getReturnType());
                arguments.add(args.getFieldValue(fieldIdEnum));
            }
        }
        Class<?>[] parameterTypeArray = parameterTypes.toArray(new Class[parameterTypes.size()]);
        Method method = obtainMethod(request.getServiceInterface(), message.name, parameterTypeArray);

        RpcInvocation invocation = new RpcInvocation(request.getServiceInterface(), method, arguments.toArray(), parameterTypeArray);
        request.setThriftMsgInfo(new ThriftMessageInfo(message.name, message.seqid));
        return invocation;
    }

    protected RpcResult doDeserializeResponse(byte[] buff, DefaultResponse response, TMessage message,
                                              TProtocol protocol) throws Exception {
        RpcResult rpcResult = new RpcResult();
        if (message.type == TMessageType.REPLY) {
            String resultClassName = ThriftUtil.generateResultClassName(response.getServiceInterface().getName(),
                    message.name);
            if (StringUtils.isEmpty(resultClassName)) {
                throw new ProtocolException("Thrift deserialize response: resultClassName is empty.");
            }

            TBase result = getClazzInstance(resultClassName);
            try {
                result.read(protocol);
            } catch (TException e) {
                throw new ProtocolException("Thrift deserialize response failed.", e);
            }

            int index = 0;
            Object realResult = null;
            while (true) {
                TFieldIdEnum fieldIdEnum = result.fieldForId(index++);
                if (fieldIdEnum == null) {
                    if (index == 1) {
                        continue;
                    }
                    break;
                }
                Object fieldValue = result.getFieldValue(fieldIdEnum);
                if (fieldValue != null) {
                    realResult = fieldValue;
                    break;
                }
            }

            if (realResult instanceof Exception) {
                response.setException((Exception) realResult);
            }
            rpcResult.setReturnVal(realResult);
        } else if (message.type == TMessageType.EXCEPTION) {
            TApplicationException exception = TApplicationException.read(protocol);
            MetaUtil.wrapException(exception.getMessage(), response);
        }
        if (!response.isOctoProtocol() && hasOldRequestHeader(protocol)) {
            RequestHeader requestHeader = new RequestHeader();
            protocol.readFieldBegin();
            requestHeader.read(protocol);
            protocol.readFieldEnd();
        }
        return rpcResult;
    }

    @Override
    protected byte[] serialize(Object obj) throws Exception {
        byte[] bodyBytes;
        if (obj instanceof DefaultRequest) {
            bodyBytes = doSerializeRequest((DefaultRequest) obj);
        } else if (obj instanceof DefaultResponse) {
            bodyBytes = doSerializeResponse((DefaultResponse) obj);
        } else {
            throw new ProtocolException("Thrift serialize  type is invalid, it should not happen.");
        }
        return bodyBytes;
    }

    private byte[] doSerializeRequest(DefaultRequest request) throws Exception {
        RpcInvocation rpcInvocation = request.getData();
        TMessage message = new TMessage(rpcInvocation.getMethod().getName(), TMessageType.CALL, request.getSeqToInt());

        String argsClassName = ThriftUtil.generateArgsClassName(
                request.getServiceInterface().getName(), rpcInvocation.getMethod().getName());
        TBase args = getClazzInstance(argsClassName);

        Object[] arguments = rpcInvocation.getArguments();
        if (arguments != null) {
            for (int i = 0; i < arguments.length; i++) {
                if (arguments[i] != null) {
                    args.setFieldValue(args.fieldForId(i + 1), arguments[i]);
                }
            }
        }

        TMemoryBuffer transport = new TMemoryBuffer(1024);
        TBinaryProtocol protocol = new TBinaryProtocol(transport);
        protocol.writeMessageBegin(message);
        if (!request.isOctoProtocol()) {
            // 不影响原生thrift解码
            RequestHeader requestHeader = MetaUtil.convertRequestToOldProtocolHeader(request);
            protocol.writeFieldBegin(MTRACE_FIELD_DESC);
            requestHeader.write(protocol);
            protocol.writeFieldEnd();
        }
        args.write(protocol);
        protocol.writeMessageEnd();
        protocol.getTransport().flush();
        return transport.getArray();
    }

    private byte[] doSerializeResponse(DefaultResponse response) throws Exception {
        ThriftMessageInfo thriftMsgInfo = response.getThriftMsgInfo();
        if (thriftMsgInfo == null) {
            throw new ProtocolException("Thrift serialize response: no thrift message info");
        }

        String resultClassName = ThriftUtil.generateResultClassName(response.getServiceInterface().getName(), thriftMsgInfo.methodName);
        TBase resultObj = getClazzInstance(resultClassName);

        TApplicationException applicationException = null;
        if (response.getException() != null) {
            Throwable exception = response.getException();
            int index = 1;
            boolean found = false;

            while (true) {
                TFieldIdEnum fieldIdEnum = resultObj.fieldForId(index++);
                if (fieldIdEnum == null) {
                    break;
                }

                String fieldName = fieldIdEnum.getFieldName();
                Class<?> clazz = cachedClass.get(resultClassName);
                Method getMethod = ThriftUtil.obtainGetMethod(clazz, fieldName);

                Throwable causeException = response.getException().getCause();
                if (causeException != null && getMethod.getReturnType().equals(causeException.getClass())) {
                    found = true;
                    resultObj.setFieldValue(fieldIdEnum, causeException);
                    break;
                }
            }

            if (!found) {
                if (exception.getCause() != null) {
                    applicationException = new TApplicationException(exception.getMessage() + ", caused by " + exception.getCause().getMessage());
                } else {
                    applicationException = new TApplicationException(exception.getMessage());
                }
            }
        } else {
            Object realResult = (response.getResult()).getReturnVal();
            if (realResult != null) {
                resultObj.setFieldValue(resultObj.fieldForId(0), realResult);
            }
        }

        TMessage message;
        if (applicationException != null) {
            message = new TMessage(thriftMsgInfo.methodName, TMessageType.EXCEPTION, thriftMsgInfo.seqId);
        } else {
            message = new TMessage(thriftMsgInfo.methodName, TMessageType.REPLY, thriftMsgInfo.seqId);
        }

        TMemoryBuffer transport = new TMemoryBuffer(1024);
        TBinaryProtocol protocol = new TBinaryProtocol(transport);
        protocol.writeMessageBegin(message);
        switch (message.type) {
            case TMessageType.EXCEPTION:
                applicationException.write(protocol);
                break;
            case TMessageType.REPLY:
                resultObj.write(protocol);
                break;
            default:
        }
        if (!response.isOctoProtocol()) {
            // 不影响原生thrift服务解码
            RequestHeader requestHeader = new RequestHeader();
            protocol.writeFieldBegin(MTRACE_FIELD_DESC);
            requestHeader.write(protocol);
            protocol.writeFieldEnd();
        }
        protocol.writeMessageEnd();
        protocol.getTransport().flush();
        return transport.getArray();
    }

    private Method obtainMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) throws NoSuchMethodException {
        String methodSignature = MethodUtil.generateMethodSignature(clazz, methodName, paramTypes);
        Method method = cachedMethod.get(methodSignature);
        if (method == null) {
            method = clazz.getMethod(methodName, paramTypes);
            cachedMethod.putIfAbsent(methodSignature, method);
        }
        return method;
    }

    private TBase getClazzInstance(String className) {
        Class clazz = cachedClass.get(className);
        if (clazz == null) {
            try {
                clazz = ClassLoaderUtil.loadClass(className);
                cachedClass.putIfAbsent(className, clazz);
            } catch (ClassNotFoundException e) {
                throw new ProtocolException("Thrift serialize request: class" + className + " load failed.", e);
            }
        }

        TBase args;
        try {
            args = (TBase) clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ProtocolException("Thrift serialize request: class" + className + " new instance failed.", e);
        }
        return args;
    }

    private boolean hasOldRequestHeader(TProtocol protocol) {
        TTransport trans = protocol.getTransport();
        if (trans.getBytesRemainingInBuffer() >= 3) {
            byte type = trans.getBuffer()[trans.getBufferPosition()];
            if (type == org.apache.thrift.protocol.TType.STRUCT) {
                short id = (short) (((trans.getBuffer()[trans.getBufferPosition() + 1] & 0xff) << 8) | ((trans.getBuffer()[trans.getBufferPosition() + 2] & 0xff)));
                if (id == MTRACE_FIELD_DESC.id) {
                    return true;
                }
            }
        }
        return false;
    }
}