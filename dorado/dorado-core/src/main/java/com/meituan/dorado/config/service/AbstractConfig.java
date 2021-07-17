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
package com.meituan.dorado.config.service;

import com.meituan.dorado.bootstrap.ExtensionInitializer;
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.common.extension.ExtensionLoader;
import com.meituan.dorado.trace.TraceFactory;
import com.meituan.dorado.util.ClazzUtil;
import java.util.List;

public abstract class AbstractConfig implements Disposable {

    protected String serviceName;
    // 服务接口, 调用端必配, 服务端若没有配置则取实现类的第一个接口
    protected Class<?> serviceInterface;

    public abstract void destroy();

    protected Class<?> getSyncIfaceInterface(Class<?> serviceInterface) {
        Class<?>[] classes = serviceInterface.getClasses();
        for (Class c : classes) {
            if (c.isMemberClass() && c.isInterface() && Constants.THRIFT_IFACE.equals(c.getSimpleName())) {
                return c;
            }
        }
        throw new IllegalArgumentException("Not find serviceInterface!");
    }

    public void loadAndInitExtensionList(AbstractConfig config, RpcRole rpcRole) {
        List<ExtensionInitializer> initializerList = ExtensionLoader.getExtensionList(ExtensionInitializer.class);
        for(ExtensionInitializer initializer : initializerList) {
            initializer.init(config, rpcRole);
        }
    }

    protected void configTrace(String appkey) {
        TraceFactory.initInvokeTrace(appkey);
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Class<?> getServiceInterface() {
        return serviceInterface;
    }

    public void setServiceInterface(Class<?> serviceInterface) {
        this.serviceInterface = serviceInterface;
    }
}
