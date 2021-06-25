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

import com.facebook.swift.codec.metadata.ThriftCatalog;
import com.facebook.swift.service.ThriftMethod;
import com.facebook.swift.service.ThriftService;
import com.google.common.base.Function;
import com.google.common.collect.*;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.facebook.swift.codec.metadata.ReflectionHelper.findAnnotatedMethods;
import static com.facebook.swift.codec.metadata.ReflectionHelper.getEffectiveClassAnnotations;

public class ThriftServiceMetadata {

    private final String name;
    private final Map<String, ThriftMethodMetadata> methods;
    private final ImmutableList<ThriftServiceMetadata> parentServices;

    public ThriftServiceMetadata(Class<?> serviceClass, ThriftCatalog catalog) {
        ThriftService thriftService = getThriftServiceAnnotation(serviceClass);
        if (thriftService.value().length() == 0) {
            name = serviceClass.getSimpleName();
        } else {
            name = thriftService.value();
        }

        ImmutableMap.Builder<String, ThriftMethodMetadata> builder = ImmutableMap.builder();

        Function<ThriftMethodMetadata, String> methodMetadataName = new Function<ThriftMethodMetadata, String>() {
            @Override
            public String apply(ThriftMethodMetadata methodMetadata) {
                return methodMetadata.getName();
            }
        };

        TreeMultimap<Integer, ThriftMethodMetadata> declaredMethods = TreeMultimap.create(
                Ordering.natural().nullsLast(), Ordering.natural().onResultOf(methodMetadataName));
        for (Method method : findAnnotatedMethods(serviceClass, ThriftMethod.class)) {
            if (method.isAnnotationPresent(ThriftMethod.class)) {
                ThriftMethodMetadata methodMetadata = new ThriftMethodMetadata(method, catalog);
                builder.put(methodMetadata.getName(), methodMetadata);
                if (method.getDeclaringClass().equals(serviceClass)) {
                    declaredMethods.put(ThriftCatalog.getMethodOrder(method), methodMetadata);
                }
            }
        }
        methods = builder.build();

        ImmutableList.Builder<ThriftServiceMetadata> parentServiceBuilder = ImmutableList.builder();
        for (Class<?> parent : serviceClass.getInterfaces()) {
            if (!getEffectiveClassAnnotations(parent, ThriftService.class).isEmpty()) {
                parentServiceBuilder.add(new ThriftServiceMetadata(parent, catalog));
            }
        }
        this.parentServices = parentServiceBuilder.build();
    }

    public String getName() {
        return name;
    }

    public ThriftMethodMetadata getMethod(String name) {
        return methods.get(name);
    }

    public Map<String, ThriftMethodMetadata> getMethods() {
        return methods;
    }

    public static ThriftService getThriftServiceAnnotation(Class<?> serviceClass) {
        Set<ThriftService> serviceAnnotations = getEffectiveClassAnnotations(serviceClass, ThriftService.class);
        return Iterables.getOnlyElement(serviceAnnotations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, methods, parentServices);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ThriftServiceMetadata other = (ThriftServiceMetadata) obj;
        return Objects.equals(this.name, other.name) && Objects.equals(this.methods, other.methods) && Objects.equals(this.parentServices, other.parentServices);
    }
}