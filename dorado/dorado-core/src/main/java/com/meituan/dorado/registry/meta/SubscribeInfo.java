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
import java.util.Map;

public class SubscribeInfo {

    private String localAppkey;

    private String remoteAppkey;

    private String serviceName;

    private String protocol;

    private String serialize;

    private String env;

    private Map<String, String> attachments = new HashMap<>();

    public String getLocalAppkey() {
        return localAppkey;
    }

    public void setLocalAppkey(String localAppkey) {
        this.localAppkey = localAppkey;
    }

    public String getRemoteAppkey() {
        return remoteAppkey;
    }

    public void setRemoteAppkey(String remoteAppkey) {
        this.remoteAppkey = remoteAppkey;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getSerialize() {
        return serialize;
    }

    public void setSerialize(String serialize) {
        this.serialize = serialize;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
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
        info.append("SubscribeInfo:{localAppkey=").append(localAppkey)
                .append(",remoteAppkey=").append(remoteAppkey)
                .append(",serviceName=").append(serviceName)
                .append(",protocol=").append(protocol)
                .append(",serialize=").append(serialize)
                .append(",env=").append(env)
                .append(",attachments=").append(attachments)
                .append("}");
        return info.toString();
    }
}
