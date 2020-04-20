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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.meituan.dorado.common.util.VersionUtil;

import java.util.List;

public class ProviderInfo {

    private String appkey;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<PortServiceInfo> serviceInfo;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<ServiceMethodInfo> serviceMethods;

    // 基本信息
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String env;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String swimlane;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String startTime;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String version = VersionUtil.getDoradoVersion();;

    public String getAppkey() {
        return appkey;
    }

    public void setAppkey(String appkey) {
        this.appkey = appkey;
    }

    public List<PortServiceInfo> getServiceInfo() {
        return serviceInfo;
    }

    public void setServiceInfo(List<PortServiceInfo> serviceInfo) {
        this.serviceInfo = serviceInfo;
    }

    public List<ServiceMethodInfo> getServiceMethods() {
        return serviceMethods;
    }

    public void setServiceMethods(List<ServiceMethodInfo> serviceMethods) {
        this.serviceMethods = serviceMethods;
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

    public String getSwimlane() {
        return swimlane;
    }

    public void setSwimlane(String swimlane) {
        this.swimlane = swimlane;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }
}
