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
package com.meituan.dorado.registry.meta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegistryInfo extends Provider {

    private List<String> serviceNames;
    private Map<String, String> attachments = new HashMap<>();

    public List<String> getServiceNames() {
        return serviceNames;
    }

    public void setServiceNames(List<String> serviceNames) {
        this.serviceNames = serviceNames;
    }

    public Map<String, String> getAttachments() {
        return attachments;
    }

    public void setAttachments(Map<String, String> attachments) {
        this.attachments = attachments;
    }

    @Override
    public String toString() {
        StringBuilder info = new StringBuilder();
        info.append("RegistryInfo:{appkey=").append(getAppkey())
                .append(",serviceNames=").append(serviceNames)
                .append(",ip=").append(getIp())
                .append(",port=").append(getPort())
                .append(",weight=").append(getWeight())
                .append(",protocol=").append(getProtocol())
                .append(",env=").append(getEnv())
                .append(",version=").append(getVersion())
                .append(",attachments=").append(attachments)
                .append("}");
        return info.toString();
    }
}
