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

package com.meituan.dorado.registry.mns;

import com.meituan.dorado.bootstrap.provider.meta.ProviderStatus;
import com.meituan.dorado.registry.ProviderListener;
import com.meituan.dorado.registry.meta.Provider;
import com.meituan.dorado.registry.meta.SubscribeInfo;
import com.octo.naming.common.thrift.model.ProtocolRequest;
import com.octo.naming.common.thrift.model.SGService;
import com.octo.naming.common.thrift.model.ServiceDetail;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MnsServiceNameDiffTest {

    private String serviceName1 = "com.meituan.dorado.api.HelloService";
    private String serviceName2 = "com.meituan.dorado.api.HelloService2";

    /**
     * MNS会将remoteAppkey 下所有节点返回
     * 推送的节点过滤掉serviceName与注册时serviceName不同的节点
     */
    @Test
    public void testChangeListenerErrorServiceName() {
        final AtomicInteger nodeNum = new AtomicInteger(0);
        ProviderListener listener = new ProviderListener() {
            @Override
            public void notify(List<Provider> list) {
                nodeNum.set(list.size());
            }

            @Override
            public void added(List<Provider> providers) {

            }

            @Override
            public void updated(List<Provider> providers) {

            }

            @Override
            public void removed(List<String> ipPorts) {

            }
        };

        MnsChangeListener mnsChangeListener = new MnsChangeListener(listener, genMnsSubscribeInfo());
        List<SGService> sgServices = genSGServices();
        mnsChangeListener.changed(genProtocolRequest(), null, sgServices, null, null, null);
        Assert.assertEquals(2, sgServices.size());
        Assert.assertEquals(1, nodeNum.get());
    }

    public SubscribeInfo genMnsSubscribeInfo() {
        SubscribeInfo mnsSubscribeInfo = new SubscribeInfo();
        mnsSubscribeInfo.setLocalAppkey("com.meituan.octo.dorado.client");
        mnsSubscribeInfo.setRemoteAppkey("com.meituan.octo.dorado.server");
        mnsSubscribeInfo.setServiceName(serviceName1);
        mnsSubscribeInfo.setProtocol("thrift");

        return mnsSubscribeInfo;
    }

    private List<SGService> genSGServices() {
        List<SGService> sgServices = new ArrayList<>();

        SGService service1 = new SGService();
        service1.setStatus(ProviderStatus.ALIVE.getCode());
        service1.setIp("127.0.0.1");
        service1.setPort(9001);
        Map<String, ServiceDetail> serviceDetailMap = new HashMap<String, ServiceDetail>();
        serviceDetailMap.put(serviceName1, new ServiceDetail(true));
        service1.setServiceInfo(serviceDetailMap);
        service1.setIp("127.0.0.1");
        sgServices.add(service1);

        SGService service2 = new SGService();
        service2.setStatus(ProviderStatus.ALIVE.getCode());
        service2.setIp("127.0.0.1");
        service2.setPort(9001);
        serviceDetailMap = new HashMap<String, ServiceDetail>();
        serviceDetailMap.put(serviceName2, new ServiceDetail(true));
        service2.setServiceInfo(serviceDetailMap);
        service2.setIp("127.0.0.1");
        sgServices.add(service2);
        return sgServices;
    }

    private ProtocolRequest genProtocolRequest() {
        SubscribeInfo subscribeInfo = genMnsSubscribeInfo();
        ProtocolRequest protocolRequest = new ProtocolRequest();
        protocolRequest.setRemoteAppkey(subscribeInfo.getRemoteAppkey());
        protocolRequest.setLocalAppkey(subscribeInfo.getLocalAppkey());
        protocolRequest.setServiceName(subscribeInfo.getServiceName());
        protocolRequest.setProtocol("thrift");
        return protocolRequest;
    }
}
