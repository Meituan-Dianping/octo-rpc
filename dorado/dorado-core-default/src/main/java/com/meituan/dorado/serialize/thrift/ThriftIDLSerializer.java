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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ThriftIDLSerializer extends ThriftMessageSerializer {
    private static final org.apache.thrift.protocol.TField MTRACE_FIELD_DESC = new org.apache.thrift.protocol.TField(
            "mtrace", org.apache.thrift.protocol.TType.STRUCT, (short) 32767);
    private static final String BYTE_ARRAY_CLASS_NAME = "[B";

    private static final ConcurrentMap<String, Constructor<?>> cachedConstructor = new ConcurrentHashMap<>();

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

    @Override
    protected Object deserialize4OctoThrift(byte[] buff, Object obj) throws Exception {
        TMemoryInputTransport transport = new TMemoryInputTransport(buff);
        TBinaryProtocol protocol = new TBinaryProtocol(transport);
        TMessage message = protocol.readMessageBegin();

        Object result;
        if (obj instanceof DefaultRequest) {
            result = doDeserializeRequest((DefaultRequest) obj, message, protocol);
        } else if (obj instanceof DefaultResponse) {
            result = doDeserializeResponse((DefaultResponse) obj, message, protocol);
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
            RpcInvocation rpcInvocation = doDeserializeRequest(request, message, protocol);
            request.setData(rpcInvocation);
            obj = request;

            rpcInvocation.putAttachment(Constants.TRACE_TIMELINE, timeline);
        } else if (message.type == TMessageType.REPLY || message.type == TMessageType.EXCEPTION) {
            DefaultResponse response = new DefaultResponse(Long.valueOf(message.seqid));
            response.setServiceInterface(iface);
            try {
                RpcResult rpcResult = doDeserializeResponse(response, message, protocol);
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

    private byte[] doSerializeRequest(DefaultRequest request) throws Exception {
        RpcInvocation rpcInvocation = request.getData();
        TMessage message = new TMessage(rpcInvocation.getMethod().getName(), TMessageType.CALL, request.getSeqToInt());

        TMemoryBuffer memoryBuffer = new TMemoryBuffer(1024);
        TBinaryProtocol protocol = new TBinaryProtocol(memoryBuffer);

        protocol.writeMessageBegin(message);
        serializeArguments(rpcInvocation, protocol);
        protocol.writeMessageEnd();

        protocol.getTransport().flush();
        return getActualBytes(memoryBuffer);
    }

    protected RpcInvocation doDeserializeRequest(DefaultRequest request, TMessage message,
                                                 TProtocol protocol) throws Exception {
        if (message.type != TMessageType.CALL) {
            throw new ProtocolException("Thrift deserialize request: message type is invalid.");
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
            request.setServiceName(ThriftUtil.getIDLClassName(request.getServiceInterface()));
        }

        List<Object> arguments = new ArrayList<>();
        List<Class<?>> parameterTypes = new ArrayList<>();
        deserializeArguments(request.getServiceInterface().getName(), message.name, protocol, arguments, parameterTypes);

        Class<?>[] parameterTypeArray = parameterTypes.toArray(new Class[parameterTypes.size()]);
        Method method = obtainMethod(request.getServiceInterface(), message.name, parameterTypeArray);

        RpcInvocation invocation = new RpcInvocation(request.getServiceInterface(), method, arguments.toArray(), parameterTypeArray);
        request.setThriftMsgInfo(thriftMessageInfo);
        return invocation;
    }

    private byte[] doSerializeResponse(DefaultResponse response) throws Exception {
        ThriftMessageInfo thriftMsgInfo = response.getThriftMsgInfo();
        if (thriftMsgInfo == null) {
            throw new ProtocolException("Thrift serialize response: no thrift message info");
        }

        String resultClassName = ThriftUtil.generateResultClassName(response.getServiceInterface().getName(), thriftMsgInfo.methodName);
        TBase resultClassObj = getClazzInstance(resultClassName);
        TApplicationException applicationException = serializeResult(response, resultClassObj);

        TMessage message;
        if (applicationException != null) {
            message = new TMessage(thriftMsgInfo.methodName, TMessageType.EXCEPTION, thriftMsgInfo.seqId);
        } else {
            message = new TMessage(thriftMsgInfo.methodName, TMessageType.REPLY, thriftMsgInfo.seqId);
        }

        TMemoryBuffer memoryBuffer = new TMemoryBuffer(1024);
        TBinaryProtocol protocol = new TBinaryProtocol(memoryBuffer);
        protocol.writeMessageBegin(message);
        switch (message.type) {
            case TMessageType.EXCEPTION:
                applicationException.write(protocol);
                break;
            case TMessageType.REPLY:
                resultClassObj.write(protocol);
                break;
            default:
        }
        if (!response.isOctoProtocol() && thriftMsgInfo.isOldProtocol()) {
            // 若请求是老协议, 添加老协议的Header
            RequestHeader requestHeader = new RequestHeader();
            protocol.writeFieldBegin(MTRACE_FIELD_DESC);
            requestHeader.write(protocol);
            protocol.writeFieldEnd();
        }
        protocol.writeMessageEnd();
        protocol.getTransport().flush();
        return getActualBytes(memoryBuffer);
    }

    protected RpcResult doDeserializeResponse(DefaultResponse response, TMessage message,
                                              TProtocol protocol) throws Exception {
        RpcResult rpcResult = new RpcResult();
        if (message.type == TMessageType.REPLY) {
            Object realResult = deserializeResult(protocol, response.getServiceInterface().getName(), message.name);
            if (realResult instanceof Exception) {
                // 服务端自定义异常
                response.setException((Exception) realResult);
            }
            rpcResult.setReturnVal(realResult);
        } else if (message.type == TMessageType.EXCEPTION) {
            TApplicationException exception = TApplicationException.read(protocol);
            MetaUtil.wrapException(exception, response);
        }
        if (!response.isOctoProtocol() && hasOldRequestHeader(protocol)) {
            // 解析老协议的Header
            RequestHeader requestHeader = new RequestHeader();
            protocol.readFieldBegin();
            requestHeader.read(protocol);
            protocol.readFieldEnd();
        }
        return rpcResult;
    }

    private void serializeArguments(RpcInvocation rpcInvocation, TBinaryProtocol protocol) throws TException {
        String argsClassName = ThriftUtil.generateArgsClassName(
                rpcInvocation.getServiceInterface().getName(), rpcInvocation.getMethod().getName());
        TBase argsClassObj = getClazzInstance(argsClassName);

        if (rpcInvocation.getArguments() == null) {
            argsClassObj.write(protocol);
            return;
        }

        String argsFieldsClassName = ThriftUtil.generateIDLFieldsClassName(argsClassName);
        Class<?> argsFieldsClazz = getClazz(argsFieldsClassName);

        Object[] fieldEnums = argsFieldsClazz.getEnumConstants();
        if (fieldEnums == null && fieldEnums.length != rpcInvocation.getArguments().length) {
            throw new ProtocolException("argument num is " + rpcInvocation.getArguments().length + " is not match with fields in " + argsFieldsClassName);
        }
        for (int i = 0; i < fieldEnums.length; i++) {
            TFieldIdEnum fieldIdEnum = (TFieldIdEnum) fieldEnums[i];
            if (fieldIdEnum == null) {
                continue;
            }
            argsClassObj.setFieldValue(fieldIdEnum, rpcInvocation.getArguments()[i]);
        }
        argsClassObj.write(protocol);
    }

    private void deserializeArguments(String ifaceName, String methodName, TProtocol protocol, List<Object> arguments, List<Class<?>> parameterTypes) {
        String argsClassName = ThriftUtil.generateArgsClassName(
                ifaceName, methodName);
        TBase argsClassObj = getClazzInstance(argsClassName);
        try {
            argsClassObj.read(protocol);
        } catch (TException e) {
            throw new ProtocolException("Thrift deserialize arguments failed.", e);
        }

        String argsFieldsClassName = ThriftUtil.generateIDLFieldsClassName(argsClassName);
        Class<?> argsFieldsClazz = getClazz(argsFieldsClassName);

        if (argsFieldsClazz.getEnumConstants() == null) {
            return;
        }
        for (Object fieldEnum : argsFieldsClazz.getEnumConstants()) {
            TFieldIdEnum fieldIdEnum = (TFieldIdEnum) fieldEnum;
            if (fieldIdEnum == null) {
                continue;
            }
            Object argument = argsClassObj.getFieldValue(fieldIdEnum);
            String fieldName = fieldIdEnum.getFieldName();
            Method getMethod = ThriftUtil.obtainGetMethod(argsClassObj.getClass(), fieldName);
            if (BYTE_ARRAY_CLASS_NAME.equals(getMethod.getReturnType().getName())) {
                parameterTypes.add(ByteBuffer.class);
                arguments.add(ByteBuffer.wrap((byte[]) argument));
            } else {
                parameterTypes.add(getMethod.getReturnType());
                arguments.add(argument);
            }
        }
    }

    private TApplicationException serializeResult(DefaultResponse response, TBase resultClassObj) {
        TApplicationException applicationException = null;
        String resultFieldsClassName = ThriftUtil.generateIDLFieldsClassName(resultClassObj.getClass().getName());
        Class<?> resultFieldsClazz = getClazz(resultFieldsClassName);
        if (response.getException() != null) {
            boolean hasIDLException = false;
            Object[] fieldEnums = resultFieldsClazz.getEnumConstants();
            if (fieldEnums != null && fieldEnums.length > 1) {
                for (int i = 1; i < fieldEnums.length; i++) {
                    TFieldIdEnum fieldIdEnum = (TFieldIdEnum) fieldEnums[i];
                    if (fieldIdEnum == null) {
                        continue;
                    }

                    String fieldName = fieldIdEnum.getFieldName();
                    Method getMethod = ThriftUtil.obtainGetMethod(resultClassObj.getClass(), fieldName);

                    Throwable causeException = response.getException().getCause();
                    if (causeException != null && getMethod.getReturnType().equals(causeException.getClass())) {
                        hasIDLException = true;
                        resultClassObj.setFieldValue(fieldIdEnum, causeException);
                        break;
                    }
                }
            }
            if (!hasIDLException) {
                Throwable exception = response.getException();
                applicationException = new TApplicationException(CommonUtil.genExceptionMessage(exception));
            }
        } else {
            Object realResult = (response.getResult()).getReturnVal();
            if (realResult != null) {
                resultClassObj.setFieldValue(resultClassObj.fieldForId(0), realResult);
            }
        }
        return applicationException;
    }

    private Object deserializeResult(TProtocol protocol, String ifaceName, String methodName) {
        String resultClassName = ThriftUtil.generateResultClassName(ifaceName, methodName);
        TBase resultClassObj = getClazzInstance(resultClassName);
        try {
            resultClassObj.read(protocol);
        } catch (TException e) {
            throw new ProtocolException("Thrift deserialize result failed.", e);
        }
        Object realResult = null;
        String resultFieldsClassName = ThriftUtil.generateIDLFieldsClassName(resultClassObj.getClass().getName());
        Class<?> resultFieldsClazz = getClazz(resultFieldsClassName);
        Object[] resultFieldsEnums = resultFieldsClazz.getEnumConstants();
        if (resultFieldsEnums == null) {
            return realResult;
        }
        // 避免基本类型默认值导致异常返回被忽略, 从后面开始获取值
        for (int i = resultFieldsEnums.length - 1; i >= 0; i--) {
            TFieldIdEnum fieldIdEnum = (TFieldIdEnum) resultFieldsEnums[i];
            if (fieldIdEnum == null) {
                continue;
            }
            Object fieldValue = resultClassObj.getFieldValue(fieldIdEnum);
            if (fieldValue != null) {
                if (BYTE_ARRAY_CLASS_NAME.equals(fieldValue.getClass().getName())) {
                    fieldValue = ByteBuffer.wrap((byte[]) fieldValue);
                }
                realResult = fieldValue;
                break;
            }
        }
        return realResult;
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
        Class clazz = getClazz(className);
        TBase args;
        try {
            args = (TBase) clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ProtocolException("Class " + className + " new instance failed.", e);
        }
        return args;
    }

    private Class<?> getClazz(String className) {
        Class<?> clazz = cachedClass.get(className);
        if (clazz == null) {
            try {
                clazz = ClassLoaderUtil.loadClass(className);
                cachedClass.putIfAbsent(className, clazz);
            } catch (ClassNotFoundException e) {
                throw new ProtocolException("Class " + className + " load failed.", e);
            }
        }
        return clazz;
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

    private byte[] getActualBytes(TMemoryBuffer memoryBuffer) {
        byte[] actualBytes = new byte[memoryBuffer.length()];
        System.arraycopy(memoryBuffer.getArray(), 0, actualBytes, 0, memoryBuffer.length());
        return actualBytes;
    }
}