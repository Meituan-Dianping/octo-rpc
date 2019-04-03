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
package com.meituan.dorado.test.thrift.filter;

import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.common.exception.FilterException;
import com.meituan.dorado.rpc.handler.filter.Filter;
import com.meituan.dorado.rpc.handler.filter.FilterHandler;
import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.rpc.meta.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * SPI配置
 */
public class ServerQpsLimitFilter implements Filter {
    private static Logger logger = LoggerFactory.getLogger(ServerQpsLimitFilter.class);
    private static boolean enable = false;
    public static AtomicInteger count = new AtomicInteger(0);

    @Override
    public RpcResult filter(RpcInvocation invocation, FilterHandler nextHandler) throws Throwable {
        if (!enable) {
            return nextHandler.handle(invocation);
        }
        FilterTest.invokeChainStr.append(this.getClass().getSimpleName());

        logger.info("ServerQpsLimitFilter");
        if (enable && count.incrementAndGet() > 5) {
            throw new FilterException("QpsLimited");
        }
        RpcResult result = nextHandler.handle(invocation);
        logger.info("ServerQpsLimitFilter end");
        return result;
    }

    @Override
    public int getPriority() {
        return 1;
    }

    public static void enable() {
        enable = true;
    }

    public static void disable() {
        enable = false;
    }

    @Override
    public RpcRole getRole() {
        return RpcRole.PROVIDER;
    }
}
