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

import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.exception.RegistryException;
import com.meituan.dorado.registry.RegistryPolicy;
import com.meituan.dorado.registry.RegistryService;
import com.meituan.dorado.registry.meta.RegistryInfo;
import com.meituan.dorado.registry.mns.util.MnsUtil;
import com.meituan.octo.mns.MnsInvoker;
import com.octo.naming.common.thrift.model.SGService;
import com.octo.naming.common.thrift.model.ServiceDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class MnsRegistryService implements RegistryService {

    private static final Logger logger = LoggerFactory.getLogger(MnsRegistryService.class);

    private final String SWIMLANE = "swimlane";

    private MnsRegistryService() {
    }

    protected static MnsRegistryService getInstance() {
        return MnsRegistryServiceHolder.INSTANCE;
    }

    private static class MnsRegistryServiceHolder {
        private static MnsRegistryService INSTANCE = new MnsRegistryService();
    }

    @Override
    public void register(RegistryInfo registryInfo) {
        try {
            SGService sgService = genSGService(registryInfo);

            MnsInvoker.registServiceWithCmd(MnsUtil.UPT_CMD_RESET, sgService);
            logger.info("Register provider on mns: {}", sgService.toString());
        } catch (Throwable e) {
            throw new RegistryException("Failed to register service: " + registryInfo.getServiceNames(), e);
        }
    }

    /**
     * 节点状态变更为 未启动
     *
     * @param registryInfo
     */
    @Override
    public void unregister(RegistryInfo registryInfo) {
        SGService sgService = genSGService(registryInfo);
        try {
            MnsInvoker.unRegisterService(sgService);
            logger.info("Unregister provider on mns: {}", sgService.toString());
        } catch (Throwable e) {
            throw new RegistryException("Failed to unregister service: " + registryInfo.getServiceNames(), e);
        }
    }

    private SGService genSGService(RegistryInfo registryInfo) {
        SGService sgService = new SGService();
        sgService.setAppkey(registryInfo.getAppkey());
        sgService.setIp(registryInfo.getIp());
        sgService.setPort(registryInfo.getPort());
        sgService.setProtocol(registryInfo.getProtocol());
        sgService.setFweight(registryInfo.getWeight());
        sgService.setWeight((int) registryInfo.getWeight());
        sgService.setVersion(registryInfo.getVersion());
        sgService.setWarmup(registryInfo.getWarmUp());
        sgService.setEnvir(Constants.EnvType.getEnvType(registryInfo.getEnv()).getEnvCode());

        sgService.setHeartbeatSupport(MnsUtil.HeartBeatType.BothSupport.getValue());
        sgService.setLastUpdateTime((int) (System.currentTimeMillis() / 1000));

        if (registryInfo.getServiceNames() == null || registryInfo.getServiceNames().isEmpty()) {
            throw new RegistryException("RegistryInfo no serviceNames, " + registryInfo);
        }

        Map<String, ServiceDetail> serviceDetailMap = new HashMap<String, ServiceDetail>();
        for (String serviceName : registryInfo.getServiceNames()) {
            serviceDetailMap.put(serviceName, new ServiceDetail(true));
        }
        sgService.setServiceInfo(serviceDetailMap);
        return sgService;
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
