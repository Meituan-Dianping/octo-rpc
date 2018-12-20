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
package com.meituan.dorado.common.extension;

import com.meituan.dorado.common.Role;
import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.common.exception.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 无法通过有参构造函数创建对象
 */
public final class ExtensionLoader {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionLoader.class);

    private static Map<Class<?>, Object> extensionMap = new ConcurrentHashMap<Class<?>, Object>();
    private static Map<Class<?>, List<?>> extensionListMap = new ConcurrentHashMap<Class<?>, List<?>>();

    private ExtensionLoader() {
    }

    public static <T> T getExtension(Class<T> clazz) {
        T extension = (T) extensionMap.get(clazz);
        if (extension == null) {
            synchronized (extensionMap) {
                extension = (T) extensionMap.get(clazz);
                if (extension == null) {
                    extension = newExtension(clazz);
                    if (extension != null) {
                        extensionMap.put(clazz, extension);
                    } else {
                        throw new RpcException("No implement of " + clazz.getName() + ", please check spi config");
                    }
                }
            }
        }
        return extension;
    }

    /**
     * 根据实现类名的前缀获取实现类
     *
     * @param clazz
     * @param name
     * @param <T>
     * @return
     */
    public static <T> T getExtensionWithName(Class<T> clazz, String name) {
        List<T> extensions = getExtensionList(clazz);

        String interfaceName = clazz.getSimpleName();
        for (T extension : extensions) {
            String className = extension.getClass().getSimpleName();
            String subClassPrefix = className.substring(0, className.indexOf(interfaceName));
            if (name.equalsIgnoreCase(subClassPrefix)) {
                return extension;
            }
        }
        throw new RpcException("Not find " + clazz.getName() + " with name " + name + ", please check spi config");
    }

    public static <T extends Role> List<T> getExtensionListByRole(Class<T> clazz, RpcRole role) {
        try {
            List<T> extensions = getExtensionList(clazz);
            List<T> result = new LinkedList<>();
            for (T t : extensions) {
                if (t != null && t.getRole() == null) {
                    throw new RpcException("Class " + t.getClass().getName() + " rpcRole is null, please check getRole function.");
                }
                if (t != null && (t.getRole().equals(role) || t.getRole().equals(RpcRole.MULTIROLE))) {
                    result.add(t);
                }
            }
            return result;
        } catch (Throwable e) {
            logger.error("Get extensions of {} failed.", clazz.getName(), e);
            throw new RpcException(e);
        }
    }

    public static <T extends Role> T getExtensionByRole(Class<T> clazz, RpcRole role) {
        try {
            List<T> extensions = getExtensionList(clazz);
            for (T t : extensions) {
                if (t != null && t.getRole() == null) {
                    throw new RpcException("Class " + t.getClass().getName() + " rpcRole is null, please check getRole function.");
                }
                if (t != null && (t.getRole().equals(role) || t.getRole().equals(RpcRole.MULTIROLE))) {
                    return t;
                }
            }
            return null;
        } catch (Throwable e) {
            logger.error("Get extensions of {} failed.", clazz.getName(), e);
            throw new RpcException(e);
        }
    }


    public static <T> List<T> getExtensionList(Class<T> clazz) {
        List<T> extensions = (List<T>) extensionListMap.get(clazz);
        if (extensions == null) {
            synchronized (extensionListMap) {
                extensions = (List<T>) extensionListMap.get(clazz);
                if (extensions == null) {
                    extensions = newExtensionList(clazz);
                    if (!extensions.isEmpty()) {
                        extensionListMap.put(clazz, extensions);
                    }
                }
            }
        }
        return extensions;
    }

    public static <T> T getNewExtension(Class<T> clazz) {
        T extension = newExtension(clazz);
        if (extension == null) {
            throw new RpcException("No implement of " + clazz.getName() + ", please check spi config");
        }
        return extension;
    }

    public static <T> List<T> newExtensionList(Class<T> clazz) {
        ServiceLoader<T> serviceLoader = ServiceLoader.load(clazz);
        List<T> extensions = new ArrayList<T>();
        for (T service : serviceLoader) {
            extensions.add(service);
        }
        return extensions;
    }


    private static <T> T newExtension(Class<T> clazz) {
        ServiceLoader<T> serviceLoader = ServiceLoader.load(clazz);
        for (T service : serviceLoader) {
            return service;
        }
        return null;
    }
}
