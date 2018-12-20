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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Objects;

public class Provider {

    private static final Logger logger = LoggerFactory.getLogger(Provider.class);
    private static final String CHANGE_SYMBOL = " -> ";

    private String appkey;

    private String ip;

    private int port;

    private double weight;

    private String protocol;

    private boolean octoProtocol;

    private String serialize;

    private String version;

    private int status;

    private String env;

    // 启动时间 秒
    private long startTime;

    private int warmUp;

    private double originWeight;

    private AtomicBoolean warmUpFinished = new AtomicBoolean();

    public Provider() {
    }

    public Provider(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.weight = 10.0;
    }

    public String getAppkey() {
        return appkey;
    }

    public void setAppkey(String appkey) {
        this.appkey = appkey;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public int getWarmUp() {
        return warmUp;
    }

    public void setWarmUp(int warmUp) {
        this.warmUp = warmUp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public boolean isOctoProtocol() {
        return octoProtocol;
    }

    public void setOctoProtocol(boolean octoProtocol) {
        this.octoProtocol = octoProtocol;
    }

    public AtomicBoolean getWarmUpFinished() {
        return warmUpFinished;
    }

    public void degrade(double degradeWeight) {
        originWeight = weight;
        weight = degradeWeight;
    }

    public double degradeRecover() {
        weight = originWeight;
        return weight;
    }

    public boolean updateIfDiff(Provider provider2) {
        StringBuilder changeInfo = new StringBuilder();
        if (status != provider2.getStatus()) {
            changeInfo.append("status[").append(status).append(CHANGE_SYMBOL).append(provider2.getStatus()).append("]").append("; ");
            status = provider2.getStatus();
        }
        if (!StringUtils.equals(protocol, provider2.getProtocol())) {
            changeInfo.append("protocol[").append(protocol).append(CHANGE_SYMBOL).append(provider2.getProtocol()).append("]").append("; ");
            protocol = provider2.getProtocol();
        }
        if (!StringUtils.equals(serialize, provider2.getSerialize())) {
            changeInfo.append("serialize[").append(serialize).append(CHANGE_SYMBOL).append(provider2.getSerialize()).append("]").append("; ");
            serialize = provider2.getSerialize();
        }
        if (Double.compare(weight, provider2.getWeight()) != 0) {
            changeInfo.append("weight[").append(weight).append(CHANGE_SYMBOL).append(provider2.getWeight()).append("]").append("; ");
            weight = provider2.getWeight();
        }
        if (startTime != provider2.getStartTime()) {
            changeInfo.append("startTime[").append(startTime).append(CHANGE_SYMBOL).append(provider2.getStartTime()).append("]").append("; ");
            startTime = provider2.getStartTime();
        }
        if (!StringUtils.equals(version, provider2.getVersion())) {
            changeInfo.append("version[").append(version).append(CHANGE_SYMBOL).append(provider2.getVersion()).append("]").append("; ");
            version = provider2.getVersion();
        }
        if (!changeInfo.toString().isEmpty()) {
            logger.info("{} changed: {}", this.getClass().getSimpleName(), changeInfo.toString());
        }
        if (changeInfo.toString().isEmpty()) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder info = new StringBuilder();
        info.append("Provider:{appkey=").append(getAppkey())
                .append(",ip=").append(getIp())
                .append(",port=").append(getPort())
                .append(",weight=").append(getWeight())
                .append(",protocol=").append(getProtocol())
                .append(",env=").append(getEnv())
                .append(",version=").append(getVersion())
                .append("}");
        return info.toString();
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + (appkey == null ? 0 : appkey.hashCode());
        result = 31 * result + (ip == null ? 0 : ip.hashCode());
        result = 31 * result + port;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Provider other = (Provider) o;
        if (!Objects.equals(appkey, other.getAppkey())) {
            return false;
        }
        if (!Objects.equals(ip, other.getIp())) {
            return false;
        }
        if (port != other.getPort()) {
            return false;
        }
        return true;
    }
}
