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
import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.rpc.meta.RpcResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class FilterTest {

    private static Logger logger = LoggerFactory.getLogger(FilterTest.class);

    public static StringBuilder invokeChainStr = new StringBuilder();

    private static StringBuilder expectClientInvokeChainStr = new StringBuilder();
    private static StringBuilder expectServerInvokeChainStr = new StringBuilder();
    private static StringBuilder expectInvokeChainStr = new StringBuilder();

    private String testStr = "I am Emma";

    @BeforeClass
    public static void init() {
        List<Filter> filters = new ArrayList<Filter>();
        filters.add(new TraceTestFilter());
        buildExpectInvokeChainStr();
    }

    @Before
    public void beforeTest() {
        ClientQpsLimitFilter.count.set(0);
        ServerQpsLimitFilter.count.set(0);
    }

    @Test
    public void testClientInvokeChain() throws Throwable {
        invokeChainStr = new StringBuilder();
        FilterHandler handler = new FilterHandler() {
            @Override
            public RpcResult handle(RpcInvocation invokeContext) throws Throwable {
                logger.info("This is actual invoke");
                return new RpcResult(invokeContext.getArguments()[0]);
            }
        };
        FilterHandler handlerChain = InvokeChainBuilder.initInvokeChain(handler, new ArrayList<Filter>(), RpcRole.INVOKER);

        Class<?>[] classes = new Class[1];
        classes[0] = String.class;
        Method method = this.getClass().getDeclaredMethod("just4GetMethod", classes);

        Object[] arguments = new Object[1];
        arguments[0] = testStr;
        Class<?>[] parameterTypes = new Class[1];
        parameterTypes[0] = testStr.getClass();

        RpcInvocation context = new RpcInvocation(this.getClass(), method, arguments, parameterTypes);
        RpcResult result = handlerChain.handle(context);
        Assert.assertEquals(arguments[0], result.getReturnVal());
        Assert.assertEquals(expectClientInvokeChainStr.toString(), invokeChainStr.toString());
    }

    @Test
    public void testServerInvokeChain() throws Throwable {
        invokeChainStr = new StringBuilder();
        FilterHandler handler = new FilterHandler() {
            @Override
            public RpcResult handle(RpcInvocation invokeContext) throws Throwable {
                return new RpcResult();
            }
        };
        FilterHandler handlerChain = InvokeChainBuilder.initInvokeChain(handler, new ArrayList<Filter>(), RpcRole.PROVIDER);

        Method method = this.getClass().getDeclaredMethod("testServerInvokeChain");

        RpcInvocation context = new RpcInvocation(this.getClass(), method, null, null);
        handlerChain.handle(context);
        Assert.assertEquals(expectServerInvokeChainStr.toString(), invokeChainStr.toString());
    }

    @Test
    public void testInvokeChain() throws Throwable {
        invokeChainStr = new StringBuilder();
        FilterHandler handler = new FilterHandler() {
            @Override
            public RpcResult handle(RpcInvocation invokeContext) throws Throwable {
                return new RpcResult();
            }
        };
        List<Filter> filters = new ArrayList<>();
        filters.add(new CustomFilter1());
        filters.add(new CustomFilter2());
        FilterHandler handlerChain = InvokeChainBuilder.initInvokeChain(handler, filters, RpcRole.PROVIDER);

        Method method = this.getClass().getMethod("testInvokeChain");

        RpcInvocation context = new RpcInvocation(this.getClass(), method, null, null);
        handlerChain.handle(context);
        Assert.assertEquals(expectInvokeChainStr.toString(), invokeChainStr.toString());
    }

    private static void buildExpectInvokeChainStr() {
        expectClientInvokeChainStr.append(TraceTestFilter.class.getSimpleName())
                .append(InvokerFilter.class.getSimpleName())
                .append(ClientQpsLimitFilter.class.getSimpleName());
        expectServerInvokeChainStr.append(TraceTestFilter.class.getSimpleName())
                .append(ServerQpsLimitFilter.class.getSimpleName())
                .append(ProviderFilter.class.getSimpleName());

        expectInvokeChainStr.append(TraceTestFilter.class.getSimpleName())
                .append(ServerQpsLimitFilter.class.getSimpleName())
                .append(ProviderFilter.class.getSimpleName())
                .append(CustomFilter2.class.getSimpleName())
                .append(CustomFilter1.class.getSimpleName());
    }

    private void just4GetMethod(String str) {
        // no
    }

}
