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
package com.meituan.dorado.rpc.handler.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.common.exception.RpcException;
import com.meituan.dorado.common.util.ClassLoaderUtil;
import com.meituan.dorado.common.util.JacksonUtils;
import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.rpc.meta.RpcResult;
import com.meituan.dorado.serialize.thrift.ThriftUtil;
import com.meituan.dorado.serialize.thrift.annotation.ThriftAnnotationManager;
import com.meituan.dorado.serialize.thrift.annotation.codec.ThriftMethodCodec;
import com.meituan.dorado.serialize.thrift.annotation.codec.ThriftServiceCodec;
import com.meituan.dorado.transport.meta.Request;
import org.apache.thrift.TEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

public class GenericProviderFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericProviderFilter.class);

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }

    @Override
    public RpcResult filter(RpcInvocation invocation, FilterHandler handler) throws Throwable {
        Request request = (Request) invocation.getAttachment(Constants.RPC_REQUEST);
        if (request == null) {
            throw new RpcException("No request info in RpcInvocation");
        }
        String genericType = request.getLocalContext(Constants.GENERIC_KEY);
        if (genericType != null) {
            invocation = doDeserializeParameters(invocation, genericType);
            RpcResult result = handler.handle(invocation);
            doSerializeResponse(genericType, result, invocation);
            return result;
        } else {
            return handler.handle(invocation);
        }
    }

    @Override
    public RpcRole getRole() {
        return RpcRole.PROVIDER;
    }

    private RpcInvocation doDeserializeParameters(RpcInvocation invocation, String genericType) throws IOException {
        Object[] arguments = invocation.getArguments();
        String methodName = (String) arguments[0];
        List<String> paramTypes = (List<String>) arguments[1];
        List<String> paramValues = (List<String>) arguments[2];

        Object[] parameters = new Object[paramValues.size()];
        Method method;
        Type[] genericParameterTypes;

        Class<?> serviceInterface = invocation.getServiceInterface();
        if (!ThriftUtil.isAnnotation(serviceInterface)) {
            ThriftUtil.generateMethodCache(serviceInterface);
            genericParameterTypes = ThriftUtil.getMethodParameterMap().get(serviceInterface).get(methodName);
            method = ThriftUtil.getMethodMap().get(serviceInterface).get(methodName);
        } else {
            ThriftServiceCodec serviceCodec = ThriftAnnotationManager.getServerCodec(invocation.getServiceInterface());
            ThriftMethodCodec methodCodec = serviceCodec.getMethodCodecByName(methodName);
            genericParameterTypes = methodCodec.getMethod().getGenericParameterTypes();
            method = methodCodec.getMethod();
        }

        if (genericParameterTypes == null) {
            throw new RpcException("genericParameterTypes is empty, methodName=" + methodName + " is not valid.");
        }

        for (int i = 0; i < genericParameterTypes.length; i++) {
            Type type = genericParameterTypes[i];
            String paramTypeStr = paramTypes != null && i < paramTypes.size()
                    ? paramTypes.get(i) : null;
            JavaType parameterType = JacksonUtils.typeFactory().constructType(type);
            try {
                parameters[i] = jsonToObject(paramTypeStr, paramValues.get(i), parameterType, genericType);
            } catch (Exception e) {
                LOGGER.warn("Deserialize param error. paramTypeStr={}", paramTypeStr, e);
            }
        }
        return buildInvocation(invocation, method, parameters);
    }

    private RpcInvocation buildInvocation(RpcInvocation invocation, Method method, Object[] parameters) {
        RpcInvocation realInvocation = new RpcInvocation(invocation.getServiceInterface(), method, parameters, method.getParameterTypes());
        realInvocation.putAttachments(invocation.getAttachments());
        Request request = (Request) invocation.getAttachment(Constants.RPC_REQUEST);
        request.setData(realInvocation);
        return realInvocation;
    }

    private Object jsonToObject(String parameterType, String value, JavaType type, String genericType) throws Exception {
        if (Constants.GenericType.JACKSON_JSON_SIMPLE.getValue().equals(genericType)) {
            //反序列化时没有过滤字段的需求，所以json-simple统一用originalMapper反序列化，防止private字段无法解析
            return JacksonUtils.originalDeserializeUnchecked(value, type);
        } else if (parameterType != null && isCollectionWithNotNativeType(type)) {
            Class<?> clazz = ClassLoaderUtil.loadClass(parameterType);
            JavaType javaType = JacksonUtils.typeFactory().constructType(clazz);
            return JacksonUtils.deserializeUnchecked(value, javaType);
        }
        return JacksonUtils.deserializeUnchecked(value, type);
    }

    private boolean isCollectionWithNotNativeType(JavaType type) {
        boolean isContainerType = type.isContainerType();
        boolean isNotNative = type.getContentType() != null &&
                (type.getContentType().isTypeOrSubTypeOf(Long.class)
                        || type.getContentType().isTypeOrSubTypeOf(Short.class)
                        || type.getContentType().isTypeOrSubTypeOf(Float.class)
                        || type.getContentType().isTypeOrSubTypeOf(Byte.class)
                        || type.getContentType().isTypeOrSubTypeOf(TEnum.class));
        return isContainerType && isNotNative;
    }

    private void doSerializeResponse(String genericType, RpcResult result, RpcInvocation invocation) throws IOException {
        Object returnVal = result.getReturnVal();

        if (Constants.GenericType.JACKSON_JSON_DEFAULT.getValue().equals(genericType)) {
            result.setReturnVal(JacksonUtils.serializeUnchecked(returnVal));
            return;
        }
        if (Constants.GenericType.JACKSON_JSON_COMMON.getValue().equals(genericType)
                || Constants.GenericType.JACKSON_JSON_SIMPLE.getValue().equals(genericType)) {
            if (enableOriginalMapper(invocation.getServiceInterface())) {
                result.setReturnVal(JacksonUtils.originalSerializeUnchecked(returnVal));
                return;
            }
            try {
                result.setReturnVal(JacksonUtils.simpleSerializeUnchecked(returnVal));
            } catch (JsonProcessingException e) {
                //simpleMapper序列化失败时使用originalMapper再序列化一次
                result.setReturnVal(JacksonUtils.originalSerializeUnchecked(returnVal));
            }
        }
    }

    private boolean enableOriginalMapper(Class<?> serviceInterface) {
        return ThriftUtil.isAnnotation(serviceInterface);
    }
}