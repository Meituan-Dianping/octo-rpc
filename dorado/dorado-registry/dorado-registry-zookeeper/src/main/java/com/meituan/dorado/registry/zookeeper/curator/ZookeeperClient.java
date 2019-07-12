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

import com.google.common.collect.Sets;
import com.meituan.dorado.registry.zookeeper.util.ZooKeeperNodeInfo;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ZookeeperClient {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperClient.class);

    private CuratorFramework client;
    private final String address;
    private final Set<StateChangeListener> stateListeners = Sets.newConcurrentHashSet();
    private final ConcurrentMap<NodeChangeListener, PathChildrenCacheListener> nodeChangeListeners = new ConcurrentHashMap<>();
    private final ConcurrentMap<NodeChangeListener, PathChildrenCache> nodeChangeWatcher = new ConcurrentHashMap<>();


    protected ZookeeperClient(final String address) {
        this.address = address;
        try {
            RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
            client = CuratorFrameworkFactory.builder().connectString(address)
                    .sessionTimeoutMs(5000)//会话超时时间
                    .connectionTimeoutMs(5000)//连接超时时间
                    .retryPolicy(retryPolicy)
                    .build();
            client.getConnectionStateListenable().addListener(new ConnectionStateListener() {
                @Override
                public void stateChanged(CuratorFramework client, ConnectionState state) {
                    if (state == ConnectionState.CONNECTED) {
                        logger.info("ZookeeperClient[{}] connected.", address);
                    } else if (state == ConnectionState.LOST) {
                        logger.warn("ZookeeperClient[{}] connection lost.", address);
                    } else if (state == ConnectionState.RECONNECTED) {
                        logger.info("ZookeeperClient[{}] reconnected.", address);
                        // 临时节点重连时需重新注册
                        ZookeeperClient.this.connStateChanged();
                        for (PathChildrenCache watcher : nodeChangeWatcher.values()) {
                            try {
                                watcher.clearAndRefresh();
                            } catch (Exception e) {
                                logger.error("ZookeeperClient[{}] watcher refresh failed.", address);
                            }
                        }
                    }
                }
            });
            client.start();
        } catch (Throwable e) {
            logger.error("ZookeeperClient[{}]  init failed.", address, e);
            throw e;
        }
    }

    public void createNode(String path, String nodeData) throws Exception {
        int i = path.lastIndexOf('/');
        if (i > 0) {
            String parentPath = path.substring(0, i);
            if (!checkExists(parentPath)) {
                createPersistent(parentPath);
            }
        }
        createEphemeral(path, nodeData);
    }

    public boolean checkExists(String path) {
        try {
            if (client.checkExists().forPath(path) != null) {
                return true;
            }
        } catch (Exception e) {
            logger.error("ZookeeperClient[{}] checkExists failed, path={}", address, path, e);
        }
        return false;
    }

    public void createEphemeral(String path, String nodeData) throws Exception {
        if (checkExists(path)) {
            return;
        }
        try {
            byte[] bytes;
            if (nodeData == null) {
                bytes = new byte[0];
            } else {
                bytes = nodeData.getBytes("UTF-8");
            }
            client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path, bytes);
        } catch (Exception e) {
            if (!(e instanceof KeeperException.NodeExistsException)) {
                logger.error("ZookeeperClient[{}] createEphemeralPath:{} failed.", address, path, e);
                throw e;
            }
        }

    }

    public void createPersistent(String path) throws Exception {
        if (checkExists(path)) {
            return;
        }
        try {
            client.create().creatingParentsIfNeeded().forPath(path);
        } catch (Exception e) {
            if (!(e instanceof KeeperException.NodeExistsException)) {
                logger.error("ZookeeperClient[{}] createPersistentPath:{} failed.", address, path, e);
                throw e;
            }
        }
    }

    public void delete(String path) throws Exception {
        if (!checkExists(path)) {
            return;
        }
        try {
            client.delete().guaranteed().deletingChildrenIfNeeded().forPath(path);
        } catch (Exception e) {
            if (!(e instanceof KeeperException.NoNodeException)) {
                logger.error("ZookeeperClient[{}] deletePath:{} failed.", address, path, e);
                throw e;
            }
        }
    }

    public synchronized void updateNodeData(String path, String nodeData) throws Exception {
        if (!checkExists(path)) {
            return;
        }
        try {
            byte[] bytes;
            if (nodeData == null) {
                bytes = new byte[0];
            } else {
                bytes = nodeData.getBytes("UTF-8");
            }
            Stat stat = new Stat();
            client.getData().storingStatIn(stat).forPath(path);
            client.setData().withVersion(stat.getVersion()).forPath(path, bytes);
        } catch (Exception e) {
            if (!(e instanceof KeeperException.NoNodeException)) {
                logger.error("ZookeeperClient[{}] updateNodeData:{} failed.", address, path, e);
                throw e;
            }
        }
    }

    public List<String> getChildren(String parentPath) throws Exception {
        try {
            return client.getChildren().forPath(parentPath);
        } catch (KeeperException.NoNodeException e) {
            return Collections.EMPTY_LIST;
        } catch (Exception e) {
            logger.error("ZookeeperClient[{}] getChildren failed, parentPath:{}.", address, parentPath, e);
            throw e;
        }
    }

    public String getNodeData(String path) throws Exception {
        try {
            byte[] bytes = client.getData().forPath(path);
            if (bytes != null) {
                return new String(bytes, "UTF-8");
            }
            return "";
        } catch (Exception e) {
            logger.error("ZookeeperClient[{}] getNodeData:{} failed.", address, path, e);
            throw e;
        }
    }

    protected void close() {
        try {
            stateListeners.clear();
            nodeChangeListeners.clear();
            for (PathChildrenCache wather : nodeChangeWatcher.values()) {
                wather.close();
            }
            nodeChangeWatcher.clear();
            client.close();
            logger.info("ZookeeperClient[{}] closed.", address);
        } catch (Exception e) {
            logger.error("ZookeeperClient[{}] close failed.", address, e);
        }
    }

    public void addStateListener(StateChangeListener listener) {
        stateListeners.add(listener);
    }

    public void removeStateListener(StateChangeListener listener) {
        stateListeners.remove(listener);
    }

    private void connStateChanged() {
        for (StateChangeListener listener : stateListeners) {
            listener.connStateChanged();
        }
    }

    public synchronized void removeChildNodeChangeListener(NodeChangeListener listener) throws IOException {
        nodeChangeListeners.remove(listener);
        PathChildrenCache watcher = nodeChangeWatcher.remove(listener);
        watcher.close();
    }

    public synchronized void addChildNodeChangeListener(final String path, final NodeChangeListener listener) {
        try {
            PathChildrenCacheListener childrenCacheListener = nodeChangeListeners.get(listener);
            if (childrenCacheListener == null) {
                PathChildrenCache watcher = nodeChangeWatcher.get(listener);
                if (watcher == null) {
                    watcher = new PathChildrenCache(client, path, true);
                    nodeChangeWatcher.put(listener, watcher);
                }
                childrenCacheListener = new PathChildrenCacheListener() {
                    @Override
                    public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                        switch (event.getType()) {
                            case CHILD_ADDED: {
                                String childNodePath = ZKPaths.getNodeFromPath(event.getData().getPath());
                                logger.info("Node added: {}", childNodePath);
                                String childPath = path + ZooKeeperNodeInfo.PATH_SEPARATOR + childNodePath;
                                listener.childNodeAdded(childPath, childNodePath);
                                break;
                            }
                            case CHILD_UPDATED: {
                                String childNodePath = ZKPaths.getNodeFromPath(event.getData().getPath());
                                logger.info("Node changed: {}", childNodePath);
                                String childPath = path + ZooKeeperNodeInfo.PATH_SEPARATOR + childNodePath;
                                listener.childNodeUpdated(childPath, childNodePath);
                                break;
                            }
                            case CHILD_REMOVED: {
                                String childNodePath = ZKPaths.getNodeFromPath(event.getData().getPath());
                                logger.info("Node removed: {}", childNodePath);
                                String childPath = path + ZooKeeperNodeInfo.PATH_SEPARATOR + childNodePath;
                                listener.childNodeRemoved(childPath, childNodePath);
                                break;
                            }
                            default:
                        }
                    }
                };

                watcher.start();
                watcher.getListenable().addListener(childrenCacheListener);
                nodeChangeListeners.put(listener, childrenCacheListener);
            }
        } catch (Exception e) {
            logger.error("ZookeeperClient addChildNodeChangeListener[{}] failed.", path, e);
        }
    }

    public String getAddress() {
        return address;
    }
}
