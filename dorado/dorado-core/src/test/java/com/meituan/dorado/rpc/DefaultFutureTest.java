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


import com.meituan.dorado.MockUtil;
import com.meituan.dorado.bootstrap.invoker.ServiceInvocationRepository;
import com.meituan.dorado.common.exception.RpcException;
import com.meituan.dorado.common.exception.TimeoutException;
import org.junit.Assert;
import org.junit.Test;


public class DefaultFutureTest {

    @Test
    public void testFutureTimeout() {
        try {
            final DefaultFuture future = (DefaultFuture) MockUtil.getFuture();
            Assert.assertTrue(future.getTimeout() == 1000);

            ServiceInvocationRepository.putRequestAndFuture(future.getRequest(), future);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        future.get();
                    } catch (Exception e) {
                        Assert.assertTrue(e instanceof TimeoutException);
                    }
                }
            }).start();

            Thread.sleep(1200);
            ServiceInvocationRepository.removeAndGetFuture(future.getRequest().getSeq())
                    .received(MockUtil.getResponse());

        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testFutureException() {
        try {
            final DefaultFuture future = (DefaultFuture) MockUtil.getFuture();
            Assert.assertTrue(future.getTimeout() == 1000);

            ServiceInvocationRepository.putRequestAndFuture(future.getRequest(), future);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        future.get();
                    } catch (Exception e) {
                        Assert.assertTrue(e.getCause() instanceof RpcException);
                    }
                }
            }).start();

            Thread.sleep(200);
            ServiceInvocationRepository.removeAndGetFuture(future.getRequest().getSeq())
                    .received(MockUtil.getErrorResponse());

        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testFutureSuccess() {
        try {
            final DefaultFuture<Integer> future = (DefaultFuture) MockUtil.getFuture();
            Assert.assertTrue(future.getTimeout() == 1000);

            ServiceInvocationRepository.putRequestAndFuture(future.getRequest(), future);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Assert.assertTrue(future.get() == 5);
                    } catch (Exception e) {
                        Assert.fail();
                    }
                }
            }).start();

            Thread.sleep(200);
            ServiceInvocationRepository.removeAndGetFuture(future.getRequest().getSeq())
                    .received(MockUtil.getResponse());

        } catch (Exception e) {
            Assert.fail();
        }
    }
}
