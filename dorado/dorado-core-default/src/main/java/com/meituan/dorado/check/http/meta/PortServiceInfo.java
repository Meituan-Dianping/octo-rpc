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
import com.meituan.dorado.bootstrap.provider.meta.ServiceIfaceInfo;

import java.util.List;

public class PortServiceInfo {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String port;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<ServiceIfaceInfo> serviceIfaceInfos;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String status;

    public PortServiceInfo(String port, List<ServiceIfaceInfo> serviceIfaceInfos, String status) {
        this.port = port;
        this.serviceIfaceInfos = serviceIfaceInfos;
        this.status = status;
    }

    public String getPort() {
        return port;
    }

    public List<ServiceIfaceInfo> getServiceIfaceInfos() {
        return serviceIfaceInfos;
    }

    public String getStatus() {
        return status;
    }
}
