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
package com.meituan.dorado.registry.zookeeper.curator;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ZookeeperManager {

    private volatile static ConcurrentMap<String, ZookeeperClient> zkClients;
    private volatile static ConcurrentMap<String, AtomicInteger> zkClientRefCount;

    public static ZookeeperClient getZkClient(String address) {
        if (zkClients == null) {
            synchronized (ZookeeperManager.class) {
                if (zkClients == null) {
                    zkClients = new ConcurrentHashMap<>();
                }
            }
        }
        ZookeeperClient zkClient = zkClients.get(address);
        if (zkClient == null) {
            synchronized (ZookeeperManager.class) {
                zkClient = zkClients.get(address);
                if (zkClient == null) {
                    zkClient = new ZookeeperClient(address);
                    zkClients.putIfAbsent(address, zkClient);
                }
            }
        }
        addRefCount(zkClient);
        return zkClient;
    }

    /**
     * 同一个地址使用一个zkClient，引用多次时，只在没有引用时关闭zkClient
     *
     * @param address
     */
    public synchronized static void closeZkClient(String address) {
        ZookeeperClient zkClient = zkClients.get(address);
        AtomicInteger refCount = zkClientRefCount.get(zkClient.getAddress());
        if (refCount == null || refCount.decrementAndGet() == 0) {
            if (zkClient != null) {
                zkClient.close();
                zkClients.remove(address);
            }
        }
    }

    private synchronized static void addRefCount(ZookeeperClient zkClient) {
        if (zkClientRefCount == null) {
            zkClientRefCount = new ConcurrentHashMap<>();
        }
        zkClientRefCount.putIfAbsent(zkClient.getAddress(), new AtomicInteger(0));
        zkClientRefCount.get(zkClient.getAddress()).incrementAndGet();
    }
}
