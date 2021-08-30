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

import com.facebook.swift.codec.metadata.ReflectionHelper;
import com.facebook.swift.service.ThriftService;
import com.google.common.collect.ImmutableMap;
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.exception.ProtocolException;
import com.meituan.dorado.common.util.ClassLoaderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ThriftUtil {

    private static final Logger logger = LoggerFactory.getLogger(ThriftUtil.class);

    private static final ConcurrentHashMap<String, Boolean> clazzTypeMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Boolean> annotationMap = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, String> setMethodNameMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Method>> getMethodNameMap = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> argsClassNameMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> resultClassNameMap = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Class<?>, ImmutableMap<String, Method>> methodMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, ImmutableMap<String, Type[]>> methodParameterMap = new ConcurrentHashMap<>();

    public static void generateMethodCache(Class<?> serviceInterface) {
        if (methodMap.containsKey(serviceInterface)) {
            return;
        }
        Map<String, Method> methodMapBuilder = new HashMap<String, Method>();
        Map<String, Type[]> methodParameterTypeMapBuilder = new HashMap<String, Type[]>();

        ImmutableMap<String, Method> methodImmutableMap;
        ImmutableMap<String, Type[]> methodParameterTypeMap;
        for (Method method : serviceInterface.getMethods()) {
            methodMapBuilder.put(method.getName(), method);
            methodParameterTypeMapBuilder.put(method.getName(), method.getGenericParameterTypes());
        }
        methodImmutableMap = ImmutableMap.<String, Method>builder().putAll(methodMapBuilder).build();
        methodParameterTypeMap = ImmutableMap.<String, Type[]>builder().putAll(methodParameterTypeMapBuilder).build();

        methodMap.put(serviceInterface, methodImmutableMap);
        methodParameterMap.put(serviceInterface, methodParameterTypeMap);
    }

    public static ConcurrentHashMap<Class<?>, ImmutableMap<String, Type[]>> getMethodParameterMap() {
        return methodParameterMap;
    }

    public static ConcurrentHashMap<Class<?>, ImmutableMap<String, Method>> getMethodMap() {
        return methodMap;
    }

    public static boolean isSupportedThrift(Class<?> clazz) {
        return (isAnnotation(clazz) || isIDL(clazz));
    }

    public static boolean isAnnotation(Class<?> clazz) {
        if (!annotationMap.containsKey(clazz)) {
            Set<ThriftService> serviceAnnotations = ReflectionHelper.getEffectiveClassAnnotations(clazz, ThriftService.class);
            if (serviceAnnotations.size() == 1) {
                annotationMap.put(clazz, true);
            } else {
                annotationMap.put(clazz, false);
                if (serviceAnnotations.size() > 1) {
                    logger.error("Service class:{} has multiple conflicting @ThriftService", clazz.getName());
                }
            }
        }
        return annotationMap.get(clazz);
    }

    public static boolean isIDL(Class<?> obj) {
        String name = obj.getName();
        int index = name.indexOf(Constants.LINK_SUB_CLASS_SYMBOL);
        if (index < 0) {
            index = name.length();
        }
        String clazzType = name.substring(0, index);
        if (!clazzTypeMap.containsKey(clazzType)) {
            Class<?> clazz = null;
            try {
                clazz = ClassLoaderUtil.loadClass(clazzType);
            } catch (ClassNotFoundException e) {
                return false;
            }
            Class<?>[] classes = clazz.getClasses();
            Set<String> classNames = new HashSet<>();
            for (Class c : classes) {
                classNames.add(c.getSimpleName());
            }
            if (classNames.contains(Constants.THRIFT_IFACE) && classNames.contains("AsyncIface")
                    && classNames.contains("Client") && classNames.contains("AsyncClient")
                    && classNames.contains("Processor")) {
                clazzTypeMap.put(clazzType, true);
            } else {
                clazzTypeMap.put(clazzType, false);
            }
        }
        return clazzTypeMap.get(clazzType);
    }

    public static String generateSetMethodName(String fieldName) {
        if (!setMethodNameMap.containsKey(fieldName)) {
            String methodName = new StringBuilder(16)
                    .append("set")
                    .append(Character.toUpperCase(fieldName.charAt(0)))
                    .append(fieldName.substring(1))
                    .toString();
            setMethodNameMap.put(fieldName, methodName);
        }
        return setMethodNameMap.get(fieldName);
    }

    public static Method obtainGetMethod(Class<?> clazz, String fieldName) {
        if (!getMethodNameMap.containsKey(clazz)) {
            getMethodNameMap.put(clazz, new ConcurrentHashMap<String, Method>());
        }
        if (!getMethodNameMap.get(clazz).containsKey(fieldName)) {
            String methodName = new StringBuffer(16)
                    .append("get")
                    .append(Character.toUpperCase(fieldName.charAt(0)))
                    .append(fieldName.substring(1))
                    .toString();
            Method getMethod;
            try {
                getMethod = clazz.getMethod(methodName);
            } catch (NoSuchMethodException e) {
                try {
                    getMethod = clazz.getMethod(ThriftUtil.generateBoolMethodName(fieldName));
                } catch (NoSuchMethodException e0) {
                    throw new ProtocolException("Cannot find get or is method of " + fieldName + " in " + clazz.getName(), e);
                }
            }
            getMethodNameMap.get(clazz).put(fieldName, getMethod);
        }
        return getMethodNameMap.get(clazz).get(fieldName);
    }

    public static String generateBoolMethodName(String fieldName) {

        return new StringBuffer(16)
                .append("is")
                .append(Character.toUpperCase(fieldName.charAt(0)))
                .append(fieldName.substring(1))
                .toString();
    }

    public static String generateMethodArgsClassName(String serviceName, String methodName) {

        int index = serviceName.indexOf(Constants.LINK_SUB_CLASS_SYMBOL);

        StringBuilder argsClassName = new StringBuilder(64);
        if (index > 0) {
            argsClassName.append(serviceName.substring(0, index + 1));
        } else {
            argsClassName.append(serviceName).append(Constants.LINK_SUB_CLASS_SYMBOL);
        }
        argsClassName.append(methodName).append("_args");
        return argsClassName.toString();
    }

    public static String generateMethodResultClassName(String serviceName, String methodName) {
        int index = serviceName.indexOf(Constants.LINK_SUB_CLASS_SYMBOL);
        StringBuilder resultClassName = new StringBuilder(64);
        if (index > 0) {
            resultClassName.append(serviceName.substring(0, index + 1));
        } else {
            resultClassName.append(serviceName).append(Constants.LINK_SUB_CLASS_SYMBOL);
        }
        resultClassName.append(methodName).append("_result");
        return resultClassName.toString();
    }

    public static String getIDLClassName(Class<?> serviceInterface) {
        if (serviceInterface.getEnclosingClass() != null) {
            return serviceInterface.getEnclosingClass().getName();
        }
        return serviceInterface.getName();
    }

    public static String generateArgsClassName(String serviceName, String methodName) {
        if (!argsClassNameMap.containsKey(serviceName)) {
            argsClassNameMap.put(serviceName, new ConcurrentHashMap<String, String>());
        }
        if (!argsClassNameMap.get(serviceName).containsKey(methodName)) {
            String className = ThriftUtil.generateMethodArgsClassName(serviceName, methodName);
            argsClassNameMap.get(serviceName).put(methodName, className);
            return className;
        }
        return argsClassNameMap.get(serviceName).get(methodName);
    }

    public static String generateResultClassName(String serviceName, String methodName) {
        if (!resultClassNameMap.containsKey(serviceName)) {
            resultClassNameMap.put(serviceName, new ConcurrentHashMap<String, String>());
        }
        if (!resultClassNameMap.get(serviceName).containsKey(methodName)) {
            String className = ThriftUtil.generateMethodResultClassName(serviceName, methodName);
            resultClassNameMap.get(serviceName).put(methodName, className);
            return className;
        }
        return resultClassNameMap.get(serviceName).get(methodName);
    }

    public static String generateIDLClientClassName(String serviceName) {
        int index = serviceName.indexOf(Constants.LINK_SUB_CLASS_SYMBOL);
        StringBuilder clientClassName = new StringBuilder(64);
        if (index > 0) {
            clientClassName.append(serviceName.substring(0, index + 1));
        } else {
            clientClassName.append(serviceName).append(Constants.LINK_SUB_CLASS_SYMBOL);
        }
        clientClassName.append("Client");
        return clientClassName.toString();
    }

    public static Class<?> getIDLClientClass(Class<?> serviceInterface) {
        Class<?> clazz = serviceInterface;
        if (clazz.isMemberClass()) {
            clazz = clazz.getEnclosingClass();
        }
        Class<?>[] classes = clazz.getClasses();
        for (Class c : classes) {
            if (c.isMemberClass() && !c.isInterface() && "Client".equals(c.getSimpleName())) {
                return c;
            }
        }
        throw new ProtocolException("serviceInterface must contain Sub Class of Client");
    }

    public static String generateClientSendKey(Class<?> clazz, String methodName) {
        return clazz + ".send_" + methodName;
    }

    public static Method getIDLClientSendMethod(Class<?> clazz, String methodName, Class<?>[] parameterTypes) {
        try {
            return clazz.getMethod("send_" + methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new ProtocolException("getIDLClientSendMethod failed.", e);
        }
    }

    public static String generateIDLFieldsClassName(String parentClassName) {
        return parentClassName + Constants.LINK_SUB_CLASS_SYMBOL + "_Fields";
    }
}
