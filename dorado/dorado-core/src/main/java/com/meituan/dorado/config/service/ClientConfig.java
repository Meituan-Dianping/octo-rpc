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

import com.meituan.dorado.common.Constants;
import com.meituan.dorado.rpc.handler.filter.Filter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 需支持动态配置
 */
public class ClientConfig extends AbstractConfig {

    private String appkey;
    private String remoteAppkey;
    private List<RemoteAddress> directConnAddress;
    private boolean remoteOctoProtocol;
    private RemoteAddress address;
    private int connTimeout;
    private int timeout;
    private Map<String, Integer> methodTimeout;
    private int failoverRetryTimes;

    private String callWay;
    private String protocol;
    // 序列化类型, 默认thrift
    private String serialize;

    private boolean enableHttpServer;
    private boolean timelineTrace;

    // 兼容bean配置, 也可以SPI配置
    private List<Filter> filters;

    public ClientConfig(ReferenceConfig config) {
        this.serviceInterface = config.getServiceInterface();
        this.serviceName = config.getServiceName();
        this.appkey = config.getAppkey();

        this.remoteAppkey = config.getRemoteAppkey();
        this.directConnAddress = genRemoteAddress(config.getDirectConnAddress());
        this.connTimeout = config.getConnTimeout();
        this.timeout = config.getTimeout();
        this.methodTimeout = config.getMethodTimeout();
        this.callWay = config.getCallWay();
        this.protocol = config.getProtocol();
        this.serialize = config.getSerialize();
        this.filters = config.getFilters();
        this.timelineTrace = config.isTimelineTrace();
        this.failoverRetryTimes = config.getFailoverRetryTimes();
        if (directConnAddress != null && !directConnAddress.isEmpty()) {
            // 直连时 remoteOctoProtocol 才生效
            this.remoteOctoProtocol = config.isRemoteOctoProtocol();
        }
    }

    public RemoteAddress getAddress() {
        return address;
    }

    public void setAddress(String ip, int port) {
        this.address = new RemoteAddress(ip, port);
    }

    public class RemoteAddress {
        String ip;
        int port;

        public RemoteAddress(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        public String getIp() {
            return ip;
        }

        public int getPort() {
            return port;
        }
    }

    private List<RemoteAddress> genRemoteAddress(String remoteAddress) {
        if (remoteAddress == null) {
            return null;
        }
        List<RemoteAddress> remoteAddresses = new ArrayList<>();
        String[] addresses = remoteAddress.split(",");
        for (String address : addresses) {
            String[] ipPort = address.split(Constants.COLON);
            if (ipPort.length < 2) {
                throw new IllegalArgumentException("directConnAddress is invalid, lack port");
            }
            String ip = ipPort[0];
            String portStr = ipPort[1];
            if (portStr.isEmpty() || !StringUtils.isNumeric(portStr)) {
                throw new IllegalArgumentException("directConnAddress is invalid, please check port");
            }
            int port = Integer.parseInt(portStr);
            RemoteAddress addressObj = new RemoteAddress(ip, port);
            remoteAddresses.add(addressObj);
        }
        return remoteAddresses;
    }

    @Override
    public void destroy() {
        // do nothing
    }

    public String getRemoteAppkey() {
        return remoteAppkey;
    }

    public int getConnTimeout() {
        return connTimeout;
    }

    public void setConnTimeout(int connTimeout) {
        this.connTimeout = connTimeout;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public List<RemoteAddress> getDirectConnAddress() {
        return directConnAddress;
    }

    public int getFailoverRetryTimes() {
        return failoverRetryTimes;
    }

    public void setFailoverRetryTimes(int failoverRetryTimes) {
        this.failoverRetryTimes = failoverRetryTimes;
    }

    public String getCallWay() {
        return callWay;
    }

    public void setCallWay(String callWay) {
        this.callWay = callWay;
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

    public void setFilters(List<Filter> filters) {
        this.filters = filters;
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public String getAppkey() {
        return appkey;
    }

    public Map<String, Integer> getMethodTimeout() {
        return methodTimeout;
    }

    public boolean isEnableHttpServer() {
        return enableHttpServer;
    }

    public void setEnableHttpServer(boolean enableHttpServer) {
        this.enableHttpServer = enableHttpServer;
    }

    public boolean isTimelineTrace() {
        return timelineTrace;
    }

    public void setTimelineTrace(boolean timelineTrace) {
        this.timelineTrace = timelineTrace;
    }

    public boolean isRemoteOctoProtocol() {
        return remoteOctoProtocol;
    }
}
