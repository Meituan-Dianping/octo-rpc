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
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.exception.RegistryException;
import com.meituan.dorado.registry.DiscoveryService;
import com.meituan.dorado.registry.ProviderListener;
import com.meituan.dorado.registry.RegistryPolicy;
import com.meituan.dorado.registry.meta.Provider;
import com.meituan.dorado.registry.meta.SubscribeInfo;
import com.meituan.octo.mns.MnsInvoker;
import com.meituan.octo.mns.listener.IServiceListChangeListener;
import com.octo.naming.common.thrift.model.ProtocolRequest;
import com.octo.naming.common.thrift.model.SGService;
import com.octo.naming.common.thrift.model.ServiceDetail;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 目前的实现是依靠mns的推送发现节点变更，考虑是否加上主动获取机制
 */
public class MnsDiscoveryService implements DiscoveryService {

    private static final Logger logger = LoggerFactory.getLogger(MnsDiscoveryService.class);

    private final ConcurrentMap<SubscribeInfo, IServiceListChangeListener> listeners = new ConcurrentHashMap<SubscribeInfo, IServiceListChangeListener>();

    private MnsDiscoveryService() {
    }

    protected static MnsDiscoveryService getInstance() {
        return MnsDiscoveryServiceHolder.INSTANCE;
    }

    private static class MnsDiscoveryServiceHolder {
        private static MnsDiscoveryService INSTANCE = new MnsDiscoveryService();
    }

    /**
     * 服务订阅
     *
     * @param subscribeInfo
     * @param notifyListener
     */
    @Override
    public synchronized void subcribe(SubscribeInfo subscribeInfo, ProviderListener notifyListener) {
        ProtocolRequest protocolRequest = genProtocolRequest(subscribeInfo);
        try {
            IServiceListChangeListener listener = listeners.get(subscribeInfo);
            if (listener == null) {
                listener = new MnsChangeListener(notifyListener, subscribeInfo);
                listeners.put(subscribeInfo, listener);
                MnsInvoker.addServiceListener(protocolRequest, listener);
                List<SGService> sgServiceList = MnsInvoker.getServiceList(protocolRequest);
                List<Provider> providerList = genProviderList(sgServiceList, subscribeInfo);
                notifyListener.notify(providerList);
                logger.info("Subscribe on mns: remoteAppkey={}, serviceName={}", subscribeInfo.getRemoteAppkey(), subscribeInfo.getServiceName());
            } else {
                logger.warn("{} has subscribed service", subscribeInfo.toString());
            }
        } catch (Throwable e) {
            MnsInvoker.removeServiceListener(protocolRequest, listeners.remove(subscribeInfo));
            throw new RegistryException("Failed to subscribe service " + subscribeInfo.getServiceName(), e);
        }
    }

    @Override
    public synchronized void unsubcribe(SubscribeInfo subscribeInfo) {
        try {
            ProtocolRequest protocolRequest = genProtocolRequest(subscribeInfo);
            MnsInvoker.removeServiceListener(protocolRequest, listeners.remove(subscribeInfo));
            logger.info("Unsubscribe on mns: remoteAppkey={}, serviceName={}", subscribeInfo.getRemoteAppkey(), subscribeInfo.getServiceName());
        } catch (Throwable e) {
            throw new RegistryException("Failed to unsubscribe service " + subscribeInfo.getServiceName(), e);
        }
    }

    private ProtocolRequest genProtocolRequest(SubscribeInfo subscribeInfo) {
        ProtocolRequest protocolRequest = new ProtocolRequest();
        protocolRequest.setRemoteAppkey(subscribeInfo.getRemoteAppkey());
        protocolRequest.setLocalAppkey(subscribeInfo.getLocalAppkey());
        protocolRequest.setServiceName(subscribeInfo.getServiceName());
        protocolRequest.setProtocol(subscribeInfo.getProtocol());
        return protocolRequest;
    }

    public static List<Provider> genProviderList(List<SGService> sgServiceList, SubscribeInfo subscribeInfo) {
        if (sgServiceList == null) {
            return Collections.emptyList();
        }
        List<Provider> providerList = new ArrayList<Provider>();
        for (SGService sgService : sgServiceList) {
            if (!checkData(sgService, subscribeInfo.getProtocol())) {
                continue;
            }
            Map<String, ServiceDetail> serviceDetailMap = sgService.getServiceInfo();
            if (serviceDetailMap != null && !serviceDetailMap.isEmpty() &&
                    !serviceDetailMap.containsKey(subscribeInfo.getServiceName())) {
                continue;
            }
            boolean isOctoProtocol = false;
            if (serviceDetailMap != null && serviceDetailMap.containsKey(subscribeInfo.getServiceName())) {
                // 仅针对有serviceInfo数据的服务校验
                ServiceDetail svcDetail = serviceDetailMap.get(subscribeInfo.getServiceName());
                if (svcDetail != null && svcDetail.isUnifiedProto()) {
                    isOctoProtocol = true;
                }
            }

            Provider provider = new Provider();
            provider.setAppkey(sgService.getAppkey());
            provider.setProtocol(subscribeInfo.getProtocol());
            provider.setOctoProtocol(isOctoProtocol);
            provider.setIp(sgService.getIp());
            provider.setPort(sgService.getPort());
            provider.setStartTime(sgService.getLastUpdateTime());
            provider.setStatus(sgService.getStatus());
            provider.setVersion(sgService.getVersion());
            provider.setWeight(sgService.getFweight());
            provider.setWarmUp(sgService.getWarmup());
            provider.setEnv(Constants.EnvType.getEnvName(sgService.getEnvir()));

            providerList.add(provider);
        }
        return providerList;
    }

    private static boolean checkData(SGService sgService, String clientProtocol) {
        if (StringUtils.isNotBlank(sgService.getProtocol()) &&
                !sgService.getProtocol().equalsIgnoreCase(clientProtocol)) {
            return false;
        }
        if (StringUtils.isBlank(sgService.getIp())) {
            return false;
        }
        if (sgService.getPort() <= 0) {
            return false;
        }
        if (sgService.getStatus() != ProviderStatus.ALIVE.getCode()) {
            return false;
        }
        return true;
    }

    @Override
    public void destroy() {
        // do nothing
    }

    @Override
    public void setRegistryPolicy(RegistryPolicy registryPolicy) {
        // do nothing
    }
}
