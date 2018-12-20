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
package com.meituan.dorado.bootstrap.invoker;

import com.meituan.dorado.mock.MockRequest;
import com.meituan.dorado.rpc.DefaultFuture;
import com.meituan.dorado.rpc.ResponseFuture;
import com.meituan.dorado.transport.meta.Request;

import org.junit.Assert;
import org.junit.Test;

public class ServiceInvocationRepositoryTest {

    @Test
    public void testTimeoutTask() throws InterruptedException {
        Request request = new MockRequest();
        ResponseFuture<Integer> future = new DefaultFuture<>(request, null, 1000);

        ServiceInvocationRepository.putRequestAndFuture(request, future);
        ServiceInvocationRepository.addTimeoutTask(request, future);
        Thread.sleep(1100);
        Assert.assertNull(ServiceInvocationRepository.getRequest(request.getSeq()));

        ServiceInvocationRepository.putRequestAndFuture(request, future);
        ServiceInvocationRepository.addTimeoutTask(request, future);
        Thread.sleep(900);
        Assert.assertEquals(ServiceInvocationRepository.getRequest(request.getSeq()), request);
    }
}
