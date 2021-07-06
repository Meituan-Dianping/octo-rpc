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
package com.meituan.dorado.serialize.thrift.annotation.metadata;

import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;
import com.facebook.swift.codec.metadata.*;
import com.facebook.swift.service.ThriftException;
import com.facebook.swift.service.ThriftMethod;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.thrift.TException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

import static com.facebook.swift.codec.metadata.FieldKind.THRIFT_FIELD;
import static com.facebook.swift.codec.metadata.ReflectionHelper.extractParameterNames;
import static com.google.common.base.Preconditions.checkState;

public class ThriftMethodMetadata {

    private final String name;
    private final ThriftType returnType;
    private final List<ThriftFieldMetadata> parameters;
    private final Method method;
    private final ImmutableMap<Short, ThriftType> exceptions;
    private final boolean oneway;

    public ThriftMethodMetadata(Method method, ThriftCatalog catalog) {
        this.method = method;
        ThriftMethod thriftMethod = method.getAnnotation(ThriftMethod.class);

        if (thriftMethod.value().length() == 0) {
            name = method.getName();
        } else {
            name = thriftMethod.value();
        }
        returnType = catalog.getThriftType(method.getGenericReturnType());

        ImmutableList.Builder<ThriftFieldMetadata> builder = ImmutableList.builder();
        Type[] parameterTypes = method.getGenericParameterTypes();
        String[] parameterNames = extractParameterNames(method);
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int index = 0; index < parameterTypes.length; index++) {
            ThriftField thriftField = null;
            for (Annotation annotation : parameterAnnotations[index]) {
                if (annotation instanceof ThriftField) {
                    thriftField = (ThriftField) annotation;
                    break;
                }
            }

            short parameterId = Short.MIN_VALUE;
            boolean isLegacyId = false;
            String parameterName = null;
            ThriftField.Requiredness parameterRequiredness = ThriftField.Requiredness.UNSPECIFIED;
            if (thriftField != null) {
                parameterId = thriftField.value();
                isLegacyId = thriftField.isLegacyId();
                parameterRequiredness = thriftField.requiredness();
                if (!thriftField.name().isEmpty()) {
                    parameterName = thriftField.name();
                }
            }
            if (parameterId == Short.MIN_VALUE) {
                parameterId = (short) (index + 1);
            }
            if (parameterName == null) {
                parameterName = parameterNames[index];
            }

            Type parameterType = parameterTypes[index];
            ThriftType thriftType = catalog.getThriftType(parameterType);

            ThriftInjection parameterInjection = new ThriftParameterInjection(parameterId, parameterName, index, parameterType);

            if (parameterRequiredness == ThriftField.Requiredness.UNSPECIFIED) {
                parameterRequiredness = ThriftField.Requiredness.NONE;
            }

            ThriftFieldMetadata fieldMetadata = new ThriftFieldMetadata(parameterId, isLegacyId, parameterRequiredness,
                    thriftType, parameterName, THRIFT_FIELD, ImmutableList.of(parameterInjection),
                    Optional.<ThriftConstructorInjection>absent(), Optional.<ThriftMethodInjection>absent(), Optional.<ThriftExtraction>absent(), Optional.<TypeCoercion>absent()
            );
            builder.add(fieldMetadata);
        }
        parameters = builder.build();
        exceptions = buildExceptionMap(catalog, thriftMethod);

        this.oneway = thriftMethod.oneway();
    }

    public String getName() {
        return name;
    }

    public ThriftType getReturnType() {
        return returnType;
    }

    public List<ThriftFieldMetadata> getParameters() {
        return parameters;
    }

    public Map<Short, ThriftType> getExceptions() {
        return exceptions;
    }

    public ThriftType getException(short id) {
        return exceptions.get(id);
    }

    public Method getMethod() {
        return method;
    }

    public boolean getOneway() {
        return oneway;
    }

    private ImmutableMap<Short, ThriftType> buildExceptionMap(ThriftCatalog catalog,
            ThriftMethod thriftMethod) {
        ImmutableMap.Builder<Short, ThriftType> exceptions = ImmutableMap.builder();
        Set<Type> exceptionTypes = new HashSet<Type>();
        int customExceptionCount = 0;

        if (thriftMethod.exception().length > 0) {
            for (ThriftException thriftException : thriftMethod.exception()) {
                exceptions.put(thriftException.id(), catalog.getThriftType(thriftException.type()));
                checkState(exceptionTypes.add(thriftException.type()), "ThriftMethod.exception contains more than one value for %s", thriftException.type());
            }
        }

        for (Class<?> exceptionClass : method.getExceptionTypes()) {
            if (exceptionClass.isAssignableFrom(TException.class)) {
                continue;
            }

            if (exceptionClass.isAnnotationPresent(ThriftStruct.class)) {
                ++customExceptionCount;

                if (!exceptionTypes.contains(exceptionClass)) {
                    checkState(customExceptionCount <= 1, "ThriftMethod.exception annotation value must be specified when more than one custom exception is thrown.");
                    exceptions.put((short) 1, catalog.getThriftType(exceptionClass));
                }
            }
        }
        return exceptions.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ThriftMethodMetadata that = (ThriftMethodMetadata) o;

        return Objects.equals(name, that.name) && Objects.equals(returnType, that.returnType) &&
                Objects.equals(parameters, that.parameters) && Objects.equals(method, that.method) &&
                Objects.equals(exceptions, that.exceptions) && Objects.equals(oneway, that.oneway);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, returnType, parameters, method, exceptions, oneway);
    }
}