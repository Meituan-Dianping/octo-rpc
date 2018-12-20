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
package com.meituan.dorado.bootstrap.provider;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Provider端服务信息
 */
public class ServerInfo {

    private static final ConcurrentHashMap<Integer, ProviderStatus> serverStatus = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, Date> serverStartTime = new ConcurrentHashMap<>();


    public static int getStatus(int port) {
        ProviderStatus status = serverStatus.get(port);
        if (status == null) {
            return ProviderStatus.DEAD.getCode();
        }
        return status.getCode();
    }

    public static ConcurrentMap<Integer, ProviderStatus> getServerStatus() {
        return serverStatus;
    }

    public static void alive(int port) {
        serverStatus.put(port, ProviderStatus.ALIVE);
        serverStartTime.putIfAbsent(port, new Date());
    }

    public static void dead(int port) {
        serverStatus.put(port, ProviderStatus.DEAD);
    }
}
