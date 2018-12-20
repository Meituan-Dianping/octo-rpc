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
package com.meituan.dorado.check.http.meta;

import com.meituan.dorado.common.util.VersionUtil;

import java.util.Map;

public class ServiceInfo {

    private String appkey;
    private Map<String, String> portStatus;
    private Map<String, ServiceIfaceInfo> serviceInfo; // <serviceName, ServiceIfaceInfo>
    private String env;
    private String startTime;
    private String version = VersionUtil.getDoradoVersion();

    public String getAppkey() {
        return appkey;
    }

    public void setAppkey(String appkey) {
        this.appkey = appkey;
    }

    public Map<String, String> getPortStatus() {
        return portStatus;
    }

    public void setPortStatus(Map<String, String> portStatus) {
        this.portStatus = portStatus;
    }

    public Map<String, ServiceIfaceInfo> getServiceInfo() {
        return serviceInfo;
    }

    public void setServiceInfo(Map<String, ServiceIfaceInfo> serviceInfo) {
        this.serviceInfo = serviceInfo;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }
}
