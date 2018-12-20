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

#ifndef CTHRIFT_SRC_CTHRIFT_UTIL_ZK_CLIENT_H_
#define CTHRIFT_SRC_CTHRIFT_UTIL_ZK_CLIENT_H_


#include "task_context.h"
#include "zk_tools.h"

namespace meituan_cthrift {

class ZKNodeData{
 public:
  ZKNodeData(const std::string path,const std::string data)
      : data_value_(data),
        data_path_(path)
  {
  }

  ZKNodeData &
  operator=(const ZKNodeData &node) {
    data_value_ = node.data_value_;
    data_path_ = node.data_path_;
    return *this;
  }
  ZKNodeData(const ZKNodeData &node) {
    data_value_ = node.data_value_;
    data_path_ = node.data_path_;
  }


  std::string data_value_;
  std::string data_path_;
};

class ZkClient {
 public:
  typedef boost::shared_ptr<TaskContext<ZkGetRequest, ZkGetResponse> >
      ZkGetContextPtr;
  typedef boost::shared_ptr<TaskContext<ZkWGetRequest, ZkWGetResponse> >
      ZkWGetContextPtr;
  typedef boost::shared_ptr<TaskContext<ZkWGetChildrenRequest,
                                        ZkWGetChildrenResponse> >
      ZkWGetChildrenContextPtr;
  typedef boost::shared_ptr<TaskContext<std::string, int> > ZkConnContextPtr;
  typedef boost::shared_ptr<TaskContext<ZkCreateRequest, int> >
      ZkCreateContextPtr;
  typedef boost::shared_ptr<TaskContext<ZkSetRequest, int> > ZkSetContextPtr;
  typedef boost::shared_ptr<TaskContext<ZkExistsRequest, int> >
      ZkExistsContextPtr;
  typedef boost::function<void(ZkGetContextPtr, muduo::CountDownLatch *)>
      ZkGetCallBack;
  typedef boost::function<void(ZkWGetContextPtr)> ZkWGetCallBack;
  typedef boost::function<void(ZkWGetChildrenContextPtr)>
      ZkWGetChildrenCallBack;
  typedef boost::function<void(ZkCreateContextPtr)> ZkCreateCallBack;
  typedef boost::function<void(ZkSetContextPtr)> ZkSetCallBack;
  typedef boost::function<void(ZkExistsContextPtr)> ZkExistsCallBack;
  typedef boost::function<void(muduo::net::EventLoop * )> ThreadInitCallback;

  typedef boost::shared_ptr<ZkExistsRequest> ZkExistsRequestPtr;
  typedef boost::shared_ptr<ZkCreateRequest> ZkCreateRequestPtr;

  ZkClient();
  ~ZkClient();

  static ZkClient *GetInstance();
  static void Destroy();

  int Init(const std::string &server, const bool &origin,
           int timeout = 10000, int retry = 3);

  // connection watcher function
  static void ConnWatcher(zhandle_t *zh, int type, int state,
                          const char *path, void *watcher_ctx);

  void SetTimeout(int timeout) { m_timeout = timeout; }

  void SetRetry(int retry) { m_retry = retry; }

  void OnReconnect();

  void OnReCreate();

  int ConnectToZk();

  int CheckZk();

  int RandSleep();

  // ZK接口封装
  int ZkGet(meituan_cthrift::ZkGetInvokeParams *req);
  int ZkWGet(meituan_cthrift::ZkWGetInvokeParams *req);
  int ZkWGetChildren(meituan_cthrift::ZkWGetChildrenInvokeParams *req);
  int ZkCreate(meituan_cthrift::ZkCreateInvokeParams *req);
  int ZkSet(meituan_cthrift::ZkSetRequest *req);
  int ZkExists(meituan_cthrift::ZkExistsRequest *req);

 private:
  zhandle_t *m_zh;        // zk handler
  std::string m_server;   // server ip list and port
  int m_timeout;          // timeout
  int m_retry;            // retry time

  unsigned int rand_try_times;
  int timeout_;   // eventloop timeout

  static ZkClient *mZkClient;
  ThreadInitCallback threadInitCallback_;

  int ZkInit();
  int ZkClose();
  int ZkGet(const char *path, int watch, std::string *buffer,
            int *buffer_len, struct Stat *stat);
  int ZkWgetChildren(const char *path, watcher_fn watch, void *watcherCtx,
                     struct String_vector *strings);
  int ZkWget(const char *path,
             watcher_fn watch,
             void *watcherCtx,
             std::string *buffer,
             int *buffer_len,
             struct Stat *stat);

  int ZkPathCreateRecursivly(std::string zk_path);

  void SetThreadInitCallback(const ThreadInitCallback &cb) {
    threadInitCallback_ = cb;
  }

  void InitZkGetThreadPool(muduo::net::EventLoop *mns_loop, int num_threads,
                           const std::string &name);
  void Init(void);

  ZkGetCallBack zk_get_cb_;
  ZkWGetCallBack zk_wget_cb_;
  ZkWGetChildrenCallBack zk_wget_children_cb_;
  ZkCreateCallBack zk_create_cb_;
  ZkSetCallBack zk_set_cb_;
  ZkExistsCallBack zk_exists_cb_;

  int HandleZkGetReq(ZkGetContextPtr context, muduo::CountDownLatch *latcher);
  int HandleZkWGetReq(ZkWGetContextPtr context);
  int HandleZkWGetChildrenReq(ZkWGetChildrenContextPtr context);
  int HandleZkCreateReq(ZkCreateContextPtr context);
  int HandleZkSetReq(ZkSetContextPtr context);
  int HandleZkExistsReq(ZkExistsContextPtr context);
  muduo::net::EventLoopThread zk_loop_thread_;
  muduo::net::EventLoopThread zk_get_loop_thread_;
  muduo::net::EventLoop *zk_loop_;
  static muduo::net::EventLoop *zk_get_loop_;
  static muduo::net::EventLoopThreadPool *zk_get_pool;
  std::vector<muduo::net::EventLoop *> pool_loops_;
  std::map<std::string, ZKNodeData> path_data_;
};

}  // namespace meituan_cthrift
#endif  // CTHRIFT_SRC_CTHRIFT_UTIL_ZK_CLIENT_H_

