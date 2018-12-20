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

import java.util.concurrent.ConcurrentHashMap;

public class ClazzUtil {

    private static final ConcurrentHashMap<Class<?>, Boolean> isMemberClazzMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Class<?>> enclosingClassMasp = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Class<?>, String> clazzSimpleNameMap = new ConcurrentHashMap<>();

    public static String getClazzSimpleName(Class<?> clazz) {
        if (!clazzSimpleNameMap.containsKey(clazz)) {
            String simpleName = clazz.isMemberClass() ? clazz.getEnclosingClass().getSimpleName() : clazz.getSimpleName();
            clazzSimpleNameMap.put(clazz, simpleName);
        }
        return clazzSimpleNameMap.get(clazz);
    }

    public static Boolean isMemberClazz(Class<?> clazz) {
        if (!isMemberClazzMap.containsKey(clazz)) {
            if (clazz.isMemberClass()) {
                isMemberClazzMap.put(clazz, true);
            } else {
                isMemberClazzMap.put(clazz, false);
            }
        }
        return isMemberClazzMap.get(clazz);
    }

    public static Class<?> getEnclosingClass(Class<?> clazz) {
        Class<?> enclosingClazz = enclosingClassMasp.get(clazz);
        if (enclosingClazz == null) {
            enclosingClazz = clazz.getEnclosingClass();
            if (enclosingClazz != null) {
                enclosingClassMasp.put(clazz, enclosingClazz);
            }
        }
        return enclosingClazz == null ? clazz : enclosingClazz;
    }
}
