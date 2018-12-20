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

import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.common.extension.ExtensionLoader;
import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.rpc.meta.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class InvokeChainBuilder {

    private static final Logger logger = LoggerFactory.getLogger(InvokeChainBuilder.class);

    // 调用端或服务端全局生效的Filter
    private static ConcurrentMap<RpcRole, List<Filter>> globalFilters = new ConcurrentHashMap<>();

    static {
        try {
            List<Filter> invokeFilters = ExtensionLoader.getExtensionListByRole(Filter.class, RpcRole.INVOKER);
            List<Filter> providerFilters = ExtensionLoader.getExtensionListByRole(Filter.class, RpcRole.PROVIDER);
            globalFilters.put(RpcRole.INVOKER, invokeFilters);
            globalFilters.put(RpcRole.PROVIDER, providerFilters);
        } catch (Throwable e) {
            logger.error("InvokeChainBuilder get filters failed, filters may won't work.", e);
        }
    }

    public static <T> FilterHandler initInvokeChain(FilterHandler actualHandler, List<Filter> cfgFilters, RpcRole role) {
        FilterHandler handler = buildInvokeChain(role, actualHandler, cfgFilters);
        return handler;
    }

    private static FilterHandler buildInvokeChain(RpcRole role, FilterHandler actualHandler, List<Filter> cfgFilters) {
        List<Filter> serviceFilters = new ArrayList<Filter>();
        if (globalFilters.get(role) != null) {
            serviceFilters.addAll(globalFilters.get(role));
        }
        if (cfgFilters != null) {
            // 避免在config中又配置了SPI的Filter或重复配置导致重复添加
            for (Filter filter : cfgFilters) {
                boolean hasExist = false;
                for (Filter existFilter : serviceFilters) {
                    if (existFilter.getClass().getName().equals(filter.getClass().getName())) {
                        hasExist = true;
                        break;
                    }
                }
                if (!hasExist) {
                    serviceFilters.add(filter);
                }
            }
        }

        Collections.sort(serviceFilters, FilterComparator.INSTANCE);
        FilterHandler first = actualHandler;
        for (final Filter filter : serviceFilters) {
            final FilterHandler next = first;
            first = new FilterHandler() {
                @Override
                public RpcResult handle(RpcInvocation invocation) throws Throwable {
                    return filter.filter(invocation, next);
                }
            };
        }
        return first;
    }

    private static class FilterComparator implements Comparator<Filter> {
        private static FilterComparator INSTANCE = new FilterComparator();

        @Override
        public int compare(Filter o1, Filter o2) {
            // 按priority顺序排序
            return Integer.compare(o1.getPriority(), o2.getPriority());
        }
    }
}
