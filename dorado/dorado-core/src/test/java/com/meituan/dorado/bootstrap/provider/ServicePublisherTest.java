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
package com.meituan.dorado.bootstrap.provider;


import com.meituan.dorado.HelloService;
import com.meituan.dorado.HelloServiceImpl;
import com.meituan.dorado.MockUtil;
import com.meituan.dorado.config.service.ProviderConfig;
import com.meituan.dorado.registry.meta.Provider;
import org.junit.Assert;
import org.junit.Test;

public class ServicePublisherTest {

    @Test
    public void testServicePublishAndUnpublish() {

        ProviderConfig config = MockUtil.getProviderConfig();
        ServicePublisher.publishService(config);

        String serviceName = HelloService.class.getName();
        Assert.assertTrue(ServicePublisher.getServiceImpl(serviceName) instanceof HelloServiceImpl);
        Assert.assertTrue(ServicePublisher.getServiceImplMap().containsKey(serviceName));
        Assert.assertTrue(ServicePublisher.getServiceInterfaceMap().containsKey(serviceName));
        Assert.assertTrue(ServicePublisher.getServiceServerMap().containsKey(9001));
        Assert.assertTrue(!ServicePublisher.getServiceServerMap().get(9001).isClosed());
        Assert.assertEquals(ServicePublisher.getInterface(serviceName), HelloService.class);
        Assert.assertEquals(ServicePublisher.getAppkey(), "com.meituan.octo.dorado.server");

        Assert.assertTrue(ServerInfo.getStatus(9001) == ProviderStatus.ALIVE.getCode());
        Assert.assertTrue(ServerInfo.getStatus(9000) == ProviderStatus.DEAD.getCode());
        Assert.assertTrue(ServerInfo.getServerStatus().size() == 1);

        ServicePublisher.unpublishService(config);
        Assert.assertNull(ServicePublisher.getServiceImpl(serviceName));
        Assert.assertNull(ServicePublisher.getServiceImplMap().get(serviceName));
        Assert.assertNull(ServicePublisher.getServiceInterfaceMap().get(serviceName));
        Assert.assertNull(ServicePublisher.getServiceServerMap().get(9001));
        Assert.assertTrue(ServerInfo.getStatus(9001) == ProviderStatus.DEAD.getCode());

        config.setAppkey("");
        try {
            ServicePublisher.publishService(config);
        } catch(Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
    }
}
