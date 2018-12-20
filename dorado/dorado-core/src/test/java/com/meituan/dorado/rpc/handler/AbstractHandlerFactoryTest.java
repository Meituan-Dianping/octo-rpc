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
package com.meituan.dorado.rpc.handler;


import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.mock.MockHandlerFactory;
import org.junit.Assert;
import org.junit.Test;

public class AbstractHandlerFactoryTest {

    @Test
    public void test() {
        MockHandlerFactory factory = new MockHandlerFactory();
        try {
            factory.getInvocationHandler(Constants.MESSAGE_TYPE_HEART, RpcRole.INVOKER);
        } catch(Exception e) {
            Assert.assertTrue(e instanceof RuntimeException);
            Assert.assertTrue(e.getMessage().contains("No implement of"));
        }

        try {
            factory.getInvocationHandler(Constants.MESSAGE_TYPE_SERVICE, RpcRole.INVOKER);
        } catch(Exception e) {
            Assert.assertTrue(e instanceof IllegalStateException);
            Assert.assertTrue(e.getMessage().contains("Can not found InvokeHandler with role"));
        }
    }
}
