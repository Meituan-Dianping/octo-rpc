/*
 * Copyright (c) 2011-2018, Meituan Dianping. All Rights Reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

#ifndef CTHRIFT_SRC_UTIL_CTHRIFT_ZK_CLIENT_H_
#define CTHRIFT_SRC_UTIL_CTHRIFT_ZK_CLIENT_H_


#include "cthrift_common.h"
#include "zk_tools.h"

namespace meituan_cthrift {

class CthriftNsImp;

class ServiceZkClient {
 public:
  explicit ServiceZkClient(CthriftNsImp *p);
  ~ServiceZkClient();
  typedef boost::function<void(muduo::net::EventLoop * )>
      ThreadInitCallback;

  int GetSrvListByProtocol(std::vector<meituan_mns::SGService> *srvlist,
                           const std::string &localAppkey,
                           const std::string &appKey,
                           const std::string &protocol,
                           bool is_watcher_callback = false);

  // TODO(All service change watcher function)
  static void ServiceByProtocolWatcher(zhandle_t *zh, int type,
                                       int state, const char *path,
                                       void *watcherCtx);

 private:
  int GetSubSrvListFromZk(
      boost::shared_ptr<std::pair<bool, std::vector<
          meituan_mns::SGService> > > *srvlist,
      ZkWGetChildrenInvokeParams *param,
      const int &index,
      const std::string &provider_path,
      const std::string &remote_appkey
  );

  void MnsThreadPoolInit();
  void InitThreadPool(muduo::net::EventLoop *mns_loop,
                      int num_threads, const std::string &name);
  void SetThreadInitCallback(const ThreadInitCallback &cb) {
    threadInitCallback_ = cb;
  }
  void Init(void);

 private:
  CthriftNsImp *plugin;
  static muduo::net::EventLoopThread s_watcher_thread;

  ThreadInitCallback threadInitCallback_;
  static muduo::net::EventLoopThreadPool *mns_zk_pool[];
  static muduo::net::EventLoop *mns_loop_pool[];
  static std::vector<muduo::net::EventLoop *> pool_watcher;
};
}  // namespace meituan_cthrift

#endif

