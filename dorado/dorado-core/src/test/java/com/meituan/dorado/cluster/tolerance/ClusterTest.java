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
package com.meituan.dorado.cluster.tolerance;


import com.meituan.dorado.MockUtil;
import com.meituan.dorado.cluster.ClusterHandler;
import com.meituan.dorado.cluster.InvokerRepository;
import com.meituan.dorado.common.exception.RpcException;
import com.meituan.dorado.rpc.meta.RpcInvocation;
import org.junit.Assert;
import org.junit.Test;

public class ClusterTest<T> {

    @Test
    public void testGetClusterHandlerType() throws Throwable {
        InvokerRepository<T> repository = MockUtil.getInvokerRepository();
        RpcInvocation invocation = MockUtil.getRpcInvocation();

        FailbackCluster failbackCluster = new FailbackCluster();
        ClusterHandler failbackClusterHandler = failbackCluster.buildClusterHandler(repository);
        Assert.assertTrue(failbackClusterHandler instanceof FailbackClusterHandler);
        Assert.assertTrue(failbackClusterHandler.getRepository().getInvokers().size() == 4);
        failbackClusterHandler.handle(invocation);

        FailoverCluster failoverCluster = new FailoverCluster();
        ClusterHandler failoverClusterHandler = failoverCluster.buildClusterHandler(repository);
        Assert.assertTrue(failoverClusterHandler instanceof FailoverClusterHandler);
        failoverClusterHandler.handle(invocation);

        FailfastCluster failfastCluster = new FailfastCluster();
        ClusterHandler failfastClusterHandler = failfastCluster.buildClusterHandler(repository);
        Assert.assertTrue(failfastClusterHandler instanceof FailfastClusterHandler);
        failfastClusterHandler.handle(invocation);

        // error invoke
        repository.getInvokers().clear();
        repository.getInvokers().add(MockUtil.getErrorInvoker());
        try {
            failoverClusterHandler.handle(invocation);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof RpcException);
        }

        try {
            failfastClusterHandler.handle(invocation);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof RpcException);
        }

        try {
            failbackClusterHandler.handle(invocation);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof RpcException);
        }

        // check failback policy whether retry failed invoke
        Thread.sleep(15000);
    }

}
