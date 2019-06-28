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
import com.meituan.dorado.bootstrap.provider.meta.ProviderStatus;
import com.meituan.dorado.config.service.ProviderConfig;
import org.junit.Assert;
import org.junit.Test;

public class ServicePublisherTest {

    @Test
    public void testServicePublishAndUnpublish() {

        ProviderConfig config = MockUtil.getProviderConfig();
        ServicePublisher.publishService(config);

        String serviceName = HelloService.class.getName();
        Assert.assertTrue(ProviderInfoRepository.getServiceImpl(serviceName) instanceof HelloServiceImpl);
        Assert.assertTrue(ProviderInfoRepository.getServiceImplMap().containsKey(serviceName));
        Assert.assertTrue(ProviderInfoRepository.getServiceIfaceMap().containsKey(serviceName));
        Assert.assertTrue(ProviderInfoRepository.getPortServerInfoMap().containsKey(9001));
        Assert.assertTrue(!ProviderInfoRepository.getPortServerInfoMap().get(9001).getServer().isClosed());
        Assert.assertEquals(ProviderInfoRepository.getInterface(serviceName), HelloService.class);
        Assert.assertEquals(ServicePublisher.getAppkey(), "com.meituan.octo.dorado.server");

        Assert.assertTrue(ProviderInfoRepository.getProviderStatus(9001) == ProviderStatus.ALIVE);
        Assert.assertTrue(ProviderInfoRepository.getProviderStatus(9000) == ProviderStatus.DEAD);
        Assert.assertTrue(ProviderInfoRepository.getPortServerInfoMap().size() == 1);

        ServicePublisher.unpublishService(config);
        Assert.assertNull(ProviderInfoRepository.getServiceImpl(serviceName));
        Assert.assertNull(ProviderInfoRepository.getServiceImplMap().get(serviceName));
        Assert.assertNull(ProviderInfoRepository.getServiceIfaceMap().get(serviceName));
        Assert.assertNull(ProviderInfoRepository.getPortServerInfoMap().get(9001));
        Assert.assertTrue(ProviderInfoRepository.getProviderStatus(9001) == ProviderStatus.DEAD);

        config.setAppkey("");
        try {
            ServicePublisher.publishService(config);
        } catch(Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
    }
}
