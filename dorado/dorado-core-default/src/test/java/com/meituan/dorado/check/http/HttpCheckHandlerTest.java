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
package com.meituan.dorado.check.http;


import com.meituan.dorado.MockUtil;
import com.meituan.dorado.common.RpcRole;
import org.junit.Assert;
import org.junit.Test;

public class HttpCheckHandlerTest {

    @Test
    public void testInvokerHttpCheck() {
        DoradoHttpCheckHandler httpCheckHandler = new DoradoHttpCheckHandler();
        httpCheckHandler.setRole(RpcRole.INVOKER);
        Assert.assertTrue(httpCheckHandler.getRole() == RpcRole.INVOKER);

        String uri = "/invoke/getInterface?param1=a&param2=b";
        byte[] content = "test http check handler".getBytes();
        httpCheckHandler.handle(MockUtil.getHttpSender(), uri, content);

        uri = "/service.info";
        content = "test http check handler".getBytes();
        httpCheckHandler.handle(MockUtil.getHttpSender(), uri, content);

        uri = "/call.info";
        content = "test http check handler".getBytes();
        httpCheckHandler.handle(MockUtil.getHttpSender(), uri, content);

        uri = "/auth.info";
        content = "test http check handler".getBytes();
        httpCheckHandler.handle(MockUtil.getHttpSender(), uri, content);
    }

    @Test
    public void testProviderHttpCheck() {
        DoradoHttpCheckHandler httpCheckHandler = new DoradoHttpCheckHandler();
        httpCheckHandler.setRole(RpcRole.PROVIDER);
        Assert.assertTrue(httpCheckHandler.getRole() == RpcRole.PROVIDER);

        String uri = "/invoke/getInterface?param1=a&param2=b";
        byte[] content = "test http check handler".getBytes();
        httpCheckHandler.handle(MockUtil.getHttpSender(), uri, content);

        uri = "/service.info";
        content = "test http check handler".getBytes();
        httpCheckHandler.handle(MockUtil.getHttpSender(), uri, content);

        uri = "/call.info";
        content = "test http check handler".getBytes();
        httpCheckHandler.handle(MockUtil.getHttpSender(), uri, content);

        uri = "/auth.info";
        content = "test http check handler".getBytes();
        httpCheckHandler.handle(MockUtil.getHttpSender(), uri, content);
    }

}
