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
package com.meituan.dorado.util;

import java.lang.reflect.Method;

public class MethodUtil {

    public static String generateMethodSignatureByMethod(Class<?> clazz, Method method) {
        return generateMethodSignature(clazz, method.getName(), method.getParameterTypes());
    }

    public static String generateMethodSignature(Class<?> clazz, String methodName, Class<?>[] parameterTypes) {
        StringBuilder builder = new StringBuilder(clazz.getName());
        builder.append(":").append(methodName).append(parameterTypesToString(parameterTypes));
        return builder.toString();
    }

    public static String generateMethodSignatureNoIfacePrefix(Method method) {
        return method.getName() + parameterTypesToString(method.getParameterTypes());
    }

    private static String parameterTypesToString(Class<?>[] parameterTypes) {
        StringBuilder buf = new StringBuilder();
        buf.append("(");
        if (parameterTypes != null) {
            for (int i = 0; i < parameterTypes.length; i++) {
                if (i > 0) {
                    buf.append(", ");
                }
                Class<?> c = parameterTypes[i];
                buf.append((c == null) ? "null" : c.getName());
            }
        }
        buf.append(")");
        return buf.toString();
    }

    public static Object getDefaultResult(Class<?> returnType) {
        if (returnType.isPrimitive()) {
            String returnTypeName = returnType.getSimpleName();
            if ("boolean".equals(returnTypeName)) {
                return false;
            } else if ("char".equals(returnTypeName)) {
                return '0';
            } else if ("byte".equals(returnTypeName)) {
                return (byte) 0;
            } else if ("short".equals(returnTypeName)) {
                return (short) 0;
            } else if ("int".equals(returnTypeName)) {
                return 0;
            } else if ("long".equals(returnTypeName)) {
                return (long) 0;
            } else if ("float".equals(returnTypeName)) {
                return (float) 0;
            } else if ("double".equals(returnTypeName)) {
                return (double) 0;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
