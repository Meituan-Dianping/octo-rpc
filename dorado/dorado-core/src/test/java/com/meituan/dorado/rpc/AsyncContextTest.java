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
package com.meituan.dorado.rpc;


import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.exception.RpcException;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class AsyncContextTest {

    @Test
    public void test() {
        final AsyncContext context = AsyncContext.getContext();
        ResponseFuture<Integer> result = context.asyncCall(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                Assert.assertTrue((Boolean) context.getAttachment(Constants.ASYNC));
                return new Integer(10);
            }
        });

        Assert.assertNull(context.getAttachment(Constants.ASYNC));
        try {
            Assert.assertTrue(result == null);
        } catch (Exception e) {
            e.printStackTrace();
           Assert.fail();
        }

        result = context.asyncCall(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                throw new Exception("mock exception");
            }
        });

        try {
            result.get();
        } catch(Exception e) {
            Assert.assertTrue(e instanceof ExecutionException);
        }
        AsyncContext.removeContext();
    }
}
