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
package com.meituan.dorado.rpc.handler.provider;


import com.meituan.dorado.MockUtil;
import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.common.exception.RpcException;
import com.meituan.dorado.transport.meta.Request;
import com.meituan.dorado.transport.meta.Response;
import org.junit.Assert;
import org.junit.Test;

public class HeartBeatInvokeHandlerTest {

    @Test
    public void testHandle() throws Exception {
        Request request = MockUtil.getHeartBeatRequest();
        ScannerHeartBeatInvokeHandler invokeHandler = new ScannerHeartBeatInvokeHandler();
        Assert.assertTrue(invokeHandler.getRole() == RpcRole.PROVIDER);

        try {
            Response response = invokeHandler.handle(request);
            Assert.assertNull(response.getResult());
        } catch(Exception e) {
            Assert.fail();
        }

        request = MockUtil.getRequest();
        try {
            invokeHandler.handle(request);
        } catch(Exception e) {
            Assert.assertTrue(e instanceof RpcException);
        }

    }
}
