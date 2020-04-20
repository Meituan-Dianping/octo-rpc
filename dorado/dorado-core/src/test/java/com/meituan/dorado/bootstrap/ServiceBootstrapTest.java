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
package com.meituan.dorado.bootstrap;

import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.RpcRole;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class ServiceBootstrapTest {

    private String registry1 = "mns";
    private String registry2 = "mns://";
    private String registry3 = "mns://?k1=v1";
    private String registry4 = "zookeeper://127.0.0.1:9000";
    private String registry5 = "zookeeper://127.0.0.1:9000?k1=v1";


    @Test
    public void testParseRegistryCfg() {
        Map<String, String> registryInfo = ServiceBootstrap.parseRegistryCfg(registry1);
        Assert.assertEquals(1, registryInfo.size());
        Assert.assertEquals("mns", registryInfo.get(Constants.REGISTRY_WAY_KEY));


        registryInfo = ServiceBootstrap.parseRegistryCfg(registry2);
        Assert.assertEquals(1, registryInfo.size());
        Assert.assertEquals("mns", registryInfo.get(Constants.REGISTRY_WAY_KEY));

        registryInfo = ServiceBootstrap.parseRegistryCfg(registry3);
        Assert.assertEquals(3, registryInfo.size());
        Assert.assertEquals("mns", registryInfo.get(Constants.REGISTRY_WAY_KEY));
        Assert.assertEquals("", registryInfo.get(Constants.REGISTRY_ADDRESS_KEY));
        Assert.assertEquals("v1", registryInfo.get("k1"));

        registryInfo = ServiceBootstrap.parseRegistryCfg(registry4);
        Assert.assertEquals(2, registryInfo.size());
        Assert.assertEquals("zookeeper", registryInfo.get(Constants.REGISTRY_WAY_KEY));
        Assert.assertEquals("127.0.0.1:9000", registryInfo.get(Constants.REGISTRY_ADDRESS_KEY));

        registryInfo = ServiceBootstrap.parseRegistryCfg(registry5);
        Assert.assertEquals(3, registryInfo.size());
        Assert.assertEquals("zookeeper", registryInfo.get(Constants.REGISTRY_WAY_KEY));
        Assert.assertEquals("127.0.0.1:9000", registryInfo.get(Constants.REGISTRY_ADDRESS_KEY));
        Assert.assertEquals("v1", registryInfo.get("k1"));

    }

    @Test
    public void testHttpServer() {
        ServiceBootstrap.initHttpServer(RpcRole.INVOKER);
        Assert.assertTrue(ServiceBootstrap.getHttpServer() != null);
    }
}