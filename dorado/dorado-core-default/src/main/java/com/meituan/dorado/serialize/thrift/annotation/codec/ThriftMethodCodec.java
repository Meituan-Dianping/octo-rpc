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
package com.meituan.dorado.serialize.thrift.annotation.codec;

import com.facebook.swift.codec.ThriftCodec;
import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.codec.internal.TProtocolReader;
import com.facebook.swift.codec.internal.TProtocolWriter;
import com.facebook.swift.codec.metadata.ThriftFieldMetadata;
import com.facebook.swift.codec.metadata.ThriftParameterInjection;
import com.facebook.swift.codec.metadata.ThriftType;
import com.google.common.base.Defaults;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Primitives;
import com.google.common.reflect.TypeToken;
import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.common.exception.ApplicationException;
import com.meituan.dorado.serialize.thrift.annotation.metadata.ThriftMethodMetadata;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TBase;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolException;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class ThriftMethodCodec {

    private static final String SUCCESS_RESPONSE_FIELD_NAME = "success";
    private static final String RESULT_SUFFIX = "_result";

    private String methodName;
    private String methodAliasName;
    private Method method;
    private String resultStructName;

    private List<ParameterCodec> clientParameterCodecs;
    private Map<Short, ThriftCodec<?>> serverParameterCodecs;

    private ThriftCodec<Object> successCodec;

    private Map<Short, ThriftCodec<Object>> clientExceptionCodecs;
    private Map<Class<?>, ExceptionCodec> serverExceptionCodecs;

    private ImmutableList<ThriftFieldMetadata> parameters;
    private Map<Short, Short> thriftParameterIdToJavaArgumentListPositionMap;

    public ThriftMethodCodec(RpcRole rpcRole, ThriftMethodMetadata methodMetadata, ThriftCodecManager codecManager) {

        methodAliasName = methodMetadata.getName();
        methodName = methodMetadata.getMethod().getName();
        successCodec = (ThriftCodec<Object>) codecManager.getCodec(methodMetadata.getReturnType());

        if (rpcRole == RpcRole.INVOKER) {
            buildClientMethodCodec(methodMetadata, codecManager);
        }

        if (rpcRole == RpcRole.PROVIDER) {
            buildServerMethodCodec(methodMetadata, codecManager);
        }
    }

    private void buildServerMethodCodec(ThriftMethodMetadata methodMetadata, ThriftCodecManager codecManager) {
        resultStructName = methodAliasName + RESULT_SUFFIX;
        method = methodMetadata.getMethod();
        parameters = ImmutableList.copyOf(methodMetadata.getParameters());

        ImmutableMap.Builder<Short, ThriftCodec<?>> builder = ImmutableMap.builder();
        for (ThriftFieldMetadata fieldMetadata : methodMetadata.getParameters()) {
            builder.put(fieldMetadata.getId(), codecManager.getCodec(fieldMetadata.getThriftType()));
        }
        serverParameterCodecs = builder.build();

        ImmutableMap.Builder<Short, Short> parameterOrderingBuilder = ImmutableMap.builder();
        short javaArgumentPosition = 0;
        for (ThriftFieldMetadata fieldMetadata : methodMetadata.getParameters()) {
            parameterOrderingBuilder.put(fieldMetadata.getId(), javaArgumentPosition++);
        }
        thriftParameterIdToJavaArgumentListPositionMap = parameterOrderingBuilder.build();

        ImmutableMap.Builder<Class<?>, ExceptionCodec> exceptions = ImmutableMap.builder();
        for (Map.Entry<Short, ThriftType> entry : methodMetadata.getExceptions().entrySet()) {
            Class<?> type = TypeToken.of(entry.getValue().getJavaType()).getRawType();
            ExceptionCodec codec = new ExceptionCodec(entry.getKey(), codecManager.getCodec(entry.getValue()));
            exceptions.put(type, codec);
        }
        serverExceptionCodecs = exceptions.build();
    }

    private void buildClientMethodCodec(ThriftMethodMetadata methodMetadata, ThriftCodecManager codecManager) {
        ParameterCodec[] parameters = new ParameterCodec[methodMetadata.getParameters().size()];
        for (ThriftFieldMetadata fieldMetadata : methodMetadata.getParameters()) {
            ThriftParameterInjection parameter = (ThriftParameterInjection) fieldMetadata.getInjections().get(0);

            ParameterCodec handler = new ParameterCodec(
                    fieldMetadata.getId(),
                    fieldMetadata.getName(),
                    (ThriftCodec<Object>) codecManager.getCodec(fieldMetadata.getThriftType()));

            parameters[parameter.getParameterIndex()] = handler;
        }
        clientParameterCodecs = ImmutableList.copyOf(parameters);

        ImmutableMap.Builder<Short, ThriftCodec<Object>> exceptions = ImmutableMap.builder();
        for (Map.Entry<Short, ThriftType> entry : methodMetadata.getExceptions().entrySet()) {
            exceptions.put(entry.getKey(), (ThriftCodec<Object>) codecManager.getCodec(entry.getValue()));
        }
        clientExceptionCodecs = exceptions.build();
    }

    public <T> void writeResponse(TProtocol out, int sequenceId, byte responseType, String responseFieldName,
            short responseFieldId, T result) throws Exception {
        ThriftCodec responseCodec;
        if (SUCCESS_RESPONSE_FIELD_NAME.equals(responseFieldName)) {
            responseCodec = successCodec;
            writeTProtocol(out, sequenceId, responseType, responseFieldName, responseFieldId, result, responseCodec);
            return;
        }

        if (result instanceof ApplicationException) {
            ApplicationException exception = (ApplicationException) result;
            if (exception.getCause() != null && exception.getCause() instanceof TBase) {
                TBase tBase = (TBase) exception.getCause();
                ExceptionCodec exceptionCodec = serverExceptionCodecs.get(tBase.getClass());
                if (exceptionCodec != null) {
                    responseFieldId = exceptionCodec.getId();
                    responseCodec = exceptionCodec.getCodec();
                    writeTProtocol(out, sequenceId, TMessageType.REPLY, responseFieldName, responseFieldId, tBase, responseCodec);
                    return;
                }
            }
        }

        out.writeMessageBegin(new TMessage(methodAliasName, TMessageType.EXCEPTION, sequenceId));
        Throwable e = (Throwable) result;
        TApplicationException exception = new TApplicationException(TApplicationException.INTERNAL_ERROR, e.getMessage());
        exception.write(out);
        out.writeMessageEnd();
        out.getTransport().flush();
    }

    public Object readResponse(final TProtocol protocol, final int sequenceId) throws Exception {
        TMessage message = protocol.readMessageBegin();
        if (message.type == TMessageType.EXCEPTION) {
            TApplicationException exception = TApplicationException.read(protocol);
            protocol.readMessageEnd();
            throw exception;
        }
        if (message.type != TMessageType.REPLY) {
            throw new TApplicationException(TApplicationException.INVALID_MESSAGE_TYPE, "Received invalid message type " + message.type + " from server");
        }
        if (!message.name.equals(methodAliasName)) {
            throw new TApplicationException(TApplicationException.WRONG_METHOD_NAME, "Wrong method name in reply: expected " + methodAliasName + " but received " + message.name);
        }
        if (message.seqid != sequenceId) {
            throw new TApplicationException(TApplicationException.BAD_SEQUENCE_ID, methodAliasName + " failed: out of sequence response");
        }

        TProtocolReader reader = new TProtocolReader(protocol);
        reader.readStructBegin();
        Object result = null;
        Exception exception = null;
        while (reader.nextField()) {
            if (reader.getFieldId() == 0) {
                result = reader.readField(successCodec);
            } else {
                ThriftCodec<Object> exceptionCodec = clientExceptionCodecs.get(reader.getFieldId());
                if (exceptionCodec != null) {
                    exception = (Exception) reader.readField(exceptionCodec);
                } else {
                    reader.skipFieldData();
                }
            }
        }
        reader.readStructEnd();

        if (exception != null) {
            throw exception;
        }
        if (successCodec.getType() == ThriftType.VOID) {
            return null;
        }
        return result;
    }

    public Object[] readArguments(TProtocol in, List<Class<?>> argumentTypeList)
            throws Exception {
        try {
            int numArgs = method.getParameterTypes().length;
            Object[] args = new Object[numArgs];
            TProtocolReader reader = new TProtocolReader(in);

            reader.readStructBegin();
            while (reader.nextField()) {
                short fieldId = reader.getFieldId();

                ThriftCodec<?> codec = serverParameterCodecs.get(fieldId);
                if (codec == null) {
                    reader.skipFieldData();
                } else {
                    args[thriftParameterIdToJavaArgumentListPositionMap.get(fieldId)] = reader.readField(codec);
                }
            }
            reader.readStructEnd();

            int argumentPosition = 0;
            for (ThriftFieldMetadata argument : parameters) {
                if (args[argumentPosition] == null) {
                    Type argumentType = argument.getThriftType().getJavaType();

                    if (argumentType instanceof Class) {
                        Class<?> argumentClass = (Class<?>) argumentType;
                        argumentTypeList.add(argumentClass);
                        argumentClass = Primitives.unwrap(argumentClass);
                        args[argumentPosition] = Defaults.defaultValue(argumentClass);
                    }
                }
                argumentPosition++;
            }

            return args;
        } catch (TProtocolException e) {
            throw new TApplicationException(TApplicationException.PROTOCOL_ERROR, e.getMessage());
        }
    }

    private <T> void writeTProtocol(TProtocol out, int sequenceId, byte responseType, String responseFieldName,
            short responseFieldId, T result, ThriftCodec responseCodec) throws Exception {
        out.writeMessageBegin(new TMessage(methodAliasName, responseType, sequenceId));
        TProtocolWriter writer = new TProtocolWriter(out);
        writer.writeStructBegin(resultStructName);
        writer.writeField(responseFieldName, responseFieldId, responseCodec, result);
        writer.writeStructEnd();
        out.writeMessageEnd();
        out.getTransport().flush();
    }

    public ParameterCodec getClientParameterCodec(int i) {
        return clientParameterCodecs.get(i);
    }

    public Method getMethod() {
        return method;
    }

    public String getMethodAliasName() {
        return methodAliasName;
    }

    public String getMethodName() {
        return methodName;
    }

    public static final class ParameterCodec {
        private final short id;
        private final String name;
        private final ThriftCodec<Object> codec;

        private ParameterCodec(short id, String name, ThriftCodec<Object> codec) {
            this.id = id;
            this.name = name;
            this.codec = codec;
        }

        public short getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public ThriftCodec<Object> getCodec() {
            return codec;
        }
    }

    public static final class ExceptionCodec {
        private final short id;
        private final ThriftCodec<Object> codec;

        private ExceptionCodec(short id, ThriftCodec<?> coded) {
            this.id = id;
            this.codec = (ThriftCodec<Object>) coded;
        }

        public short getId() {
            return id;
        }

        public ThriftCodec<Object> getCodec() {
            return codec;
        }
    }
}
