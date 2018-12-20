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

#include "zk_client.h"

namespace meituan_cthrift {
const int kMaxPendingTasks = 1000;
const int kZkGetRetry = 3;
ZkClient *ZkClient::mZkClient = NULL;
muduo::net::EventLoopThreadPool *ZkClient::zk_get_pool = NULL;
muduo::net::EventLoop *ZkClient::zk_get_loop_ = NULL;

ZkClient::ZkClient()
    : m_zh(NULL),
      m_server(""),
      m_timeout(ZK_CLIENT_TIMEOUT),
      m_retry(ZK_RETRY),
      rand_try_times(0),
      timeout_(DEFAULT_SERVICE_TIMEOUT),
      zk_get_cb_(boost::bind(&ZkClient::HandleZkGetReq, this, _1, _2)),
      zk_wget_cb_(boost::bind(&ZkClient::HandleZkWGetReq, this, _1)),
      zk_wget_children_cb_(
          boost::bind(&ZkClient::HandleZkWGetChildrenReq, this, _1)),
      zk_create_cb_(boost::bind(&ZkClient::HandleZkCreateReq,
                                this, _1)),
      zk_set_cb_(boost::bind(&ZkClient::HandleZkSetReq, this, _1)),
      zk_exists_cb_(boost::bind(&ZkClient::HandleZkExistsReq,
                                this, _1)),
      zk_loop_(NULL) {
}

ZkClient::~ZkClient() {
  ZkClose();
}

void ZkClient::Destroy() {
  SAFE_DELETE(mZkClient);
}

int ZkClient::CheckZk() {
  int count = 0;
  int flag = -1;
  int state = zoo_state(m_zh);
  do {
    if (ZOO_CONNECTED_STATE == state) {
      flag = 0;
      break;
    } else if (ZOO_CONNECTING_STATE == state) {
      // 如果连接已经创建，正在建立连接时，sleep 50ms,再check
      CTHRIFT_LOG_WARN("WARN zk connection: ZOO_CONNECTING_STATE!");
      usleep(ZK_DEFAULT_RETRY_SLEEP_US);
    } else {
      CTHRIFT_LOG_ERROR("ERR zk connection lost! zk state = " << state);
      flag = -1;
    }
    state = zoo_state(m_zh);
    count++;
  } while (state != ZOO_CONNECTED_STATE && count < m_retry);

  return flag;
}

int ZkClient::Init(const std::string &server, const bool &origin,
                   int timeout, int retry) {
  rand_try_times = 0;
  m_server = server;
  m_timeout = timeout;
  m_retry = retry;
  // set log level.//
  zoo_set_debug_level(ZOO_LOG_LEVEL_WARN);

  zk_loop_ = zk_loop_thread_.startLoop();
  zk_get_loop_ = zk_get_loop_thread_.startLoop();
  zk_get_loop_->runInLoop(boost::bind(&ZkClient::InitZkGetThreadPool,
                                      this, zk_get_loop_, 1, "zk-g"));

  int ret = ConnectToZk();
  return ret;
}

int ZkClient::ConnectToZk() {
  // 先关闭连接
  ZkClose();

  int count = 0;
  // 避免跟zookeeper的自定义的状态重合
  int state = 888;
  // 默认初始值
  int delay_time = 1;
  do {
    CTHRIFT_LOG_WARN("start to connect ZK, count : " << count);
    ++count;
    if (m_zh == NULL) {
      int ret = ZkInit();
      if (0 != ret || m_zh == NULL || errno == EINVAL) {
        CTHRIFT_LOG_ERROR(
            "zookeeper_init failed. retrying in 1 second. "
                "serverList: " << m_server
                               << ", errno = " << errno
                               << ", timeout = " << m_timeout
                               << ", count = " << count);
      } else {
        CTHRIFT_LOG_DEBUG("zookeeper_init success. serverList: "
                              << m_server);
      }
    }

    // 连接建立后，sleep 1s
    sleep(delay_time);
    delay_time = delay_time * 2;

    // check状态
    state = zoo_state(m_zh);
    if (state == ZOO_CONNECTING_STATE) {
      // sleep 5ms再check 状态
      usleep(ZK_DEFAULT_RETRY_SLEEP_US);
      state = zoo_state(m_zh);
    }
  } while (state != ZOO_CONNECTED_STATE && count < m_retry + 1);

  if (state != ZOO_CONNECTED_STATE) {
    CTHRIFT_LOG_ERROR(
        "zookeeper_init failed, please check zk_server. "
            "state = " << state);
  }

  return state == ZOO_CONNECTED_STATE ? 0 : -2;
}

/*
 * ZK连接watcher, 当连接发送变化时，进行日志报警
 *
 */
void ZkClient::ConnWatcher(zhandle_t *zh, int type, int state,
                           const char *path, void *watcher_ctx) {
  if (ZOO_CONNECTED_STATE == state) {
    CTHRIFT_LOG_INFO("ConnWatcher() "
                         << "ZOO_CONNECTED_STATE = " << state
                         << ", type " << type);
  } else if (ZOO_AUTH_FAILED_STATE == state) {
    CTHRIFT_LOG_ERROR("ConnWatcher() "
                          << "ZOO_AUTH_FAILED_STATE = " << state
                          << ", type " << type);
  } else if (ZOO_EXPIRED_SESSION_STATE == state) {
    CTHRIFT_LOG_ERROR("ConnWatcher() "
                          << "ZOO_EXPIRED_SESSION_STATE = " << state
                          << ", type " << type);
    mZkClient->OnReconnect();
  } else if (ZOO_CONNECTING_STATE == state) {
    CTHRIFT_LOG_ERROR("ConnWatcher() "
                          << "ZOO_CONNECTING_STATE = " << state
                          << ", type " << type);
  } else if (ZOO_ASSOCIATING_STATE == state) {
    CTHRIFT_LOG_ERROR("ConnWatcher() "
                          << "ZOO_ASSOCIATING_STATE = " << state
                          << ", type " << type);
  }
}

int ZkClient::RandSleep() {
  srand(static_cast<unsigned int>(time(0)));
  int rand_time = 0;
  ++rand_try_times;
  rand_try_times = rand_try_times > 3 ? 3 : rand_try_times;

  // 根据次数随机一个时间，防止所有机器同一时间去重连ZK
  unsigned int seed = 3;
  switch (rand_try_times) {
    case 1: {
      rand_time = rand_r(&seed) % 10;
      break;
    }
    case 2: {
      rand_time = rand_r(&seed) % 30;
      break;
    }
    default: {
      rand_time = rand_r(&seed) % 50;
    }
  }

  usleep(rand_time * MILLISENCOND_COUNT_IN_SENCOND);
  CTHRIFT_LOG_ERROR("zk connection lost, randtimes "
                        << rand_try_times << ", wait " << rand_time
                        << "ms, start to reconnect!");

  return MNS_SUCCESS;
}

/* 
 *  获取ZK handle， 如果连接不可用, 启动重连 
*  */
void ZkClient::OnReconnect() {
  while (CheckZk() < 0) {
    RandSleep();
    if (ConnectToZk() < 0) {
      CTHRIFT_LOG_ERROR("reconnect to zk fail!");
    } else {
      // 连接建立后，rand的次数恢复到0
      rand_try_times = 0;
      OnReCreate();
    }
  }
}

/*
 *  重新连接后，重新建立临时节点
*  */
void ZkClient::OnReCreate(){
  std::map<std::string, ZKNodeData> data = path_data_;
  std::map<std::string, ZKNodeData>::iterator it = data.begin();
  for(; it != data.end(); it++) {
    ZkCreateRequest zk_create_req;
    ZkCreateInvokeParams zk_create_params;
    zk_create_req.path = it->second.data_path_;
    zk_create_req.ephemeral = true;
    zk_create_req.value = it->second.data_value_;
    zk_create_req.value_len = static_cast<int>(it->second.data_value_.size());
    zk_create_params.zk_create_request = zk_create_req;
    int ret = 0;
    ret = ZkCreate(&zk_create_params);
    if (ZOK != ret || ret == -1) {
      CTHRIFT_LOG_ERROR("OnReCreate failed,"
                            << " path: " << it->second.data_path_
                            << " value: " << it->second.data_value_
                            << ", ret = " << ret);
    } else {
      CTHRIFT_LOG_DEBUG("OnReCreate SUCCESS,"
                            << " path: " << it->second.data_path_
                            << " value: " << it->second.data_value_
                            << ", ret = " << ret);
    }
  }
}

/* 
 *  获取ZK单例对象 
 *  */
ZkClient *ZkClient::GetInstance() {
  if (NULL == mZkClient) {
    mZkClient = new ZkClient();
  }
  return mZkClient;
}

int ZkClient::ZkGet(const char *path, int watch, std::string *buffer,
                    int *buffer_len, struct Stat *stat) {
  if (CheckZk() < 0) {
    return ERR_ZK_CONNECTION_LOSS;
  }
  int retry = 0;
  int ret = 0;
  boost::shared_array<char> buff_ptr(new char[*buffer_len]);
  while (kZkGetRetry > retry++) {
    ret = zoo_get(m_zh, path, watch, buff_ptr.get(), buffer_len, stat);
    if (ZOK != ret) {
      CTHRIFT_LOG_ERROR("failed to zoo get, path: " << path
                                                    << ", buffer_len: "
                                                    << *buffer_len
                                                    << ", ret = " << ret);
      return ret;
    }
    if (*buffer_len != stat->dataLength && stat->dataLength != 0) {
      CTHRIFT_LOG_INFO("new larger buffer's size to get zk_get node");
      buff_ptr = boost::shared_array<char>(new char[stat->dataLength]);
      *buffer_len = stat->dataLength;
      continue;
    }
    break;
  }
  buffer->assign(buff_ptr.get(), *buffer_len);
  return ret;
}

int
ZkClient::ZkWgetChildren(const char *path,
                         watcher_fn watch,
                         void *watcherCtx,
                         struct String_vector *strings) {
  if (CheckZk() < 0) {
    return ERR_ZK_CONNECTION_LOSS;
  }

  int ret = zoo_wget_children(m_zh, path, watch, watcherCtx, strings);

  return ret;
}

int ZkClient::ZkWget(const char *path, watcher_fn watch,
                     void *watcherCtx, std::string *buffer,
                     int *buffer_len, struct Stat *stat) {
  if (CheckZk() < 0) {
    return ERR_ZK_CONNECTION_LOSS;
  }

  int retry = 0;
  int ret = 0;
  boost::shared_array<char> buff_ptr(new char[*buffer_len]);
  while (kZkGetRetry > retry++) {
    ret = zoo_wget(m_zh, path, watch, watcherCtx, buff_ptr.get(),
                   buffer_len, stat);
    if (ZOK != ret) {
      return ret;
    }

    if (*buffer_len != stat->dataLength && stat->dataLength != 0) {
      CTHRIFT_LOG_INFO("buffer's size is not enough to put zk_wget node");
      buff_ptr = boost::shared_array<char>(new char[stat->dataLength]);
      *buffer_len = stat->dataLength;
      continue;
    }
    break;
  }
  buffer->assign(buff_ptr.get(), *buffer_len);

  return ret;
}

// CPU高，去多线程操作
int ZkClient::ZkGet(meituan_cthrift::ZkGetInvokeParams *req) {
  int ret = 0;
  ZkGetContextPtr context(
      new TaskContext<ZkGetRequest, ZkGetResponse>
          (req->zk_get_request));
  int thread_num = 2;

  int index = req->zk_get_request.zk_index % thread_num;
  size_t pending_tasks_size = pool_loops_[index]->queueSize();
  if (static_cast<int>(pending_tasks_size) < kMaxPendingTasks) {
    muduo::CountDownLatch latcher(1);
    pool_loops_[index]->runInLoop(boost::bind(zk_get_cb_, context,
                                              &latcher));
    latcher.wait();
  } else {
    CTHRIFT_LOG_ERROR("zk backend thread overload, task queue size: "
                          << pending_tasks_size);
    return ERR_FAILEDSENDMSG;
  }
  if (NULL == context->get_response()) {
    CTHRIFT_LOG_WARN("(ZkGet) don't get response in time=" << timeout_
                                                           << "us, path = "
                                                           << req->zk_get_request.path
                                                           << ", current queuesize = "
                                                           << pending_tasks_size);
    ret = ERR_ZK_EVENTLOOP_TIMEOUT;
  } else {
    req->zk_get_response = *(context->get_response());
    // use zk errorcode as the return.
    ret = req->zk_get_response.err_code;
  }
  return ret;
}

int ZkClient::ZkWGet(meituan_cthrift::ZkWGetInvokeParams *req) {
  int ret = 0;

  ZkWGetContextPtr context(
      new TaskContext<ZkWGetRequest, ZkWGetResponse>
          (req->zk_wget_request));

  size_t pending_tasks_size = zk_loop_->queueSize();
  if (static_cast<int>(pending_tasks_size) < kMaxPendingTasks) {
    zk_loop_->runInLoop(boost::bind(zk_wget_cb_, context));
  } else {
    CTHRIFT_LOG_ERROR("zk backend thread overload, task queue size: "
                          << pending_tasks_size);
    return ERR_FAILEDSENDMSG;
  }

  context->WaitResult(timeout_);
  if (NULL == context->get_response()) {
    CTHRIFT_LOG_WARN("(ZkWGet) don't get response in time =" << timeout_
                                                             << "us, path = "
                                                             << req->zk_wget_request.path
                                                             << ", current queuesize = "
                                                             << pending_tasks_size);
    ret = ERR_ZK_EVENTLOOP_TIMEOUT;
  } else {
    req->zk_wget_response = *(context->get_response());
    ret = req->zk_wget_response.err_code;
  }

  return ret;
}

int ZkClient::ZkWGetChildren(meituan_cthrift::ZkWGetChildrenInvokeParams *req) {
  int ret = 0;
  req->zk_wgetchildren_response.count = 0;
  ZkWGetChildrenContextPtr context(
      new TaskContext<ZkWGetChildrenRequest, ZkWGetChildrenResponse>(
          req->zk_wgetchildren_request));

  size_t pending_tasks_size = zk_loop_->queueSize();

  if (static_cast<int>(pending_tasks_size) < kMaxPendingTasks) {
    zk_loop_->runInLoop(boost::bind(zk_wget_children_cb_, context));
  } else {
    CTHRIFT_LOG_ERROR("zk backend thread overload, task queue size: "
                          << pending_tasks_size);
    return ERR_FAILEDSENDMSG;
  }

  // wait for return
  context->WaitResult(timeout_);
  if (NULL == context->get_response()) {
    CTHRIFT_LOG_WARN("(ZkWGetChildren) don't get response in time="
                         << timeout_ << "us, "
        "path = " << req->zk_wgetchildren_request.path
                         << ", current queuesize =" << pending_tasks_size);
    ret = ERR_ZK_EVENTLOOP_TIMEOUT;
  } else {
    req->zk_wgetchildren_response = *(context->get_response());
    ret = req->zk_wgetchildren_response.err_code;
  }

  return ret;
}

int ZkClient::ZkCreate(meituan_cthrift::ZkCreateInvokeParams *req) {
  int ret = 0;

  ZkCreateContextPtr context(
      new TaskContext<ZkCreateRequest, int>(req->zk_create_request));

  size_t pending_tasks_size = zk_loop_->queueSize();

  if (static_cast<int>(pending_tasks_size) < kMaxPendingTasks) {
    zk_loop_->runInLoop(boost::bind(zk_create_cb_, context));
  } else {
    CTHRIFT_LOG_ERROR("zk backend thread overload, task queue size: "
                          << pending_tasks_size);
    return ERR_FAILEDSENDMSG;
  }

  ret = ERR_ZK_EVENTLOOP_TIMEOUT;
  // wait for return
  context->WaitResult(timeout_);
  if (NULL == context->get_response()) {
    CTHRIFT_LOG_WARN("(ZkCreate) don't get response in time=" << timeout_
                                                              << "us, path = "
                                                              << req->zk_create_request.path);
    ret = ERR_ZK_EVENTLOOP_TIMEOUT;
  } else if (ZOK != *(context->get_response())) {
    CTHRIFT_LOG_WARN("failed to create zk node, "
                         << "path = "
                         << req->zk_create_request.path
                         << ", ret = "
                         << context->get_response());
    ret = *context->get_response();
  } else {
    CTHRIFT_LOG_INFO("succeed to get node exists info, "
                         << "path = " << req->zk_create_request.path);
    ret = 0;
  }

  return ret;
}

int ZkClient::ZkSet(meituan_cthrift::ZkSetRequest *req) {
  int ret = 0;

  ZkSetContextPtr context(
      new TaskContext<ZkSetRequest, int>(*req));

  size_t pending_tasks_size = zk_loop_->queueSize();
  if (static_cast<int>(pending_tasks_size) < kMaxPendingTasks) {
    zk_loop_->runInLoop(boost::bind(zk_set_cb_, context));
  } else {
    CTHRIFT_LOG_ERROR("zk backend thread overload, task queue size: "
                          << pending_tasks_size);
    return ERR_FAILEDSENDMSG;
  }
  // wait for return
  context->WaitResult(timeout_);
  if (NULL == context->get_response()) {
    CTHRIFT_LOG_WARN("(ZkSet) don't get response in time="
                         << timeout_ << "us, path = " << req->path);
    ret = ERR_ZK_EVENTLOOP_TIMEOUT;
  } else if (ZOK != *(context->get_response())) {
    CTHRIFT_LOG_WARN("failed to set zk node, "
                         << "path = "
                         << req->path
                         << ", ret = "
                         << context->get_response());
    ret = *context->get_response();
  } else {
    CTHRIFT_LOG_INFO("succeed to set zk note, path = " << req->path);
    ret = 0;
  }

  return ret;
}

int ZkClient::ZkExists(meituan_cthrift::ZkExistsRequest *req) {
  int ret = 0;

  ZkExistsContextPtr context(
      new TaskContext<ZkExistsRequest, int>(*req));

  size_t pending_tasks_size = zk_loop_->queueSize();

  if (static_cast<int>(pending_tasks_size) < kMaxPendingTasks) {
    zk_loop_->runInLoop(boost::bind(zk_exists_cb_, context));
  } else {
    CTHRIFT_LOG_ERROR("zk backend thread overload, task queue size: "
                          << pending_tasks_size);
    return ERR_FAILEDSENDMSG;
  }

  ret = ERR_ZK_EVENTLOOP_TIMEOUT;
  // wait for return
  context->WaitResult(timeout_);
  if (NULL == context->get_response()) {
    CTHRIFT_LOG_WARN("(ZkExists) don't get response in time="
                         << timeout_ << "us, path = " << req->path);
    ret = ERR_ZK_EVENTLOOP_TIMEOUT;
  } else if (ZOK != *(context->get_response())) {
    CTHRIFT_LOG_WARN("failed to check zk node, "
                         << "path = "
                         << req->path
                         << ", ret = "
                         << context->get_response());
    ret = *context->get_response();
  } else {
    CTHRIFT_LOG_INFO("succeed to call zkexist, path = " << req->path);
    ret = 0;
  }

  return ret;
}

int ZkClient::ZkInit() {
  m_zh = zookeeper_init(m_server.c_str(), ConnWatcher,
                        m_timeout, 0, NULL, 0);
  int ret = (NULL == m_zh) ? -1 : 0;
  return ret;
}

int ZkClient::ZkClose() {
  int ret = 0;
  if (m_zh) {
    ret = zookeeper_close(m_zh);
    if (0 != ret) {
      CTHRIFT_LOG_ERROR("close ZK connection failed! ret = " << ret);
    }
    m_zh = NULL;
  }
  return ret;
}

int ZkClient::HandleZkGetReq(ZkGetContextPtr context,
                             muduo::CountDownLatch *latcher) {
  meituan_cthrift::ZkGetResponse zk_get_response;
  zk_get_response.buffer_len = kZkContentSize;
  int ret = ZkGet(context->get_request()->path.c_str(), 0,
                  &(zk_get_response.buffer),
                  &(zk_get_response.buffer_len),
                  &(zk_get_response.stat));
  // recorde the zk errorcode
  zk_get_response.err_code = ret;
  if (NULL != context.get()) {
    context->set_response(zk_get_response);
  }
  latcher->countDown();
  return ret;
}

int ZkClient::HandleZkWGetReq(ZkWGetContextPtr context) {
  meituan_cthrift::ZkWGetResponse zk_wget_response;
  zk_wget_response.buffer_len = kZkContentSize;
  int ret = ZkWget(context->get_request()->path.c_str(),
                   context->get_request()->watch,
                   context->get_request()->watcherCtx,
                   &(zk_wget_response.buffer),
                   &(zk_wget_response.buffer_len),
                   &(zk_wget_response.stat));

  // recorde the zk errorcode
  zk_wget_response.err_code = ret;
  if (NULL != context.get()) {
    context->set_response(zk_wget_response);
  }
  return ret;
}

int ZkClient::HandleZkWGetChildrenReq(ZkWGetChildrenContextPtr context) {
  meituan_cthrift::ZkWGetChildrenResponse zk_wgetchildren_response;
  struct String_vector stat;
  stat.count = 0;
  stat.data = 0;
  int ret = ZkWgetChildren(context->get_request()->path.c_str(),
                           context->get_request()->watch,
                           context->get_request()->watcherCtx,
                           &stat);
  // record the zk errorcode
  zk_wgetchildren_response.err_code = ret;
  if (ZOK == ret) {
    zk_wgetchildren_response.count = stat.count;
    for (int i = 0; i < stat.count; i++) {
      std::string data = stat.data[i];
      zk_wgetchildren_response.data.push_back(data);
    }
  }
  context->set_response(zk_wgetchildren_response);
  deallocate_String_vector(&stat);
  return ret;
}

int ZkClient::HandleZkCreateReq(ZkCreateContextPtr context) {
  int ret = 0;
  if (CheckZk() < 0) {
    ret = ERR_ZK_CONNECTION_LOSS;
    context->set_response(ret);
    return ret;
  }

  char path_buffer[MAX_BUF_SIZE] = {0};
  int path_buffer_len = MAX_BUF_SIZE;

  int node_type = ZOO_EPHEMERAL;
  if( ! context->get_request()->ephemeral ){
    node_type = 0;
    ret = ZkPathCreateRecursivly(context->get_request()->path);
  }else{
    ZKNodeData data(context->get_request()->path,
               context->get_request()->value);

    std::map<std::string, ZKNodeData>::iterator it = path_data_.find
        (context->get_request()->path);
    if(it != path_data_.end()){
      path_data_.erase(it);
      path_data_.insert(make_pair(context->get_request()->path, data));
    }else{
      path_data_.insert(make_pair(context->get_request()->path, data));
    };

    ret = zoo_create(m_zh,
                     context->get_request()->path.c_str(),
                     context->get_request()->value.c_str(),
                     context->get_request()->value_len,
                     &ZOO_OPEN_ACL_UNSAFE /* use ACL of parent */,
                     node_type ,
                     path_buffer,
                     path_buffer_len);
  }

  CTHRIFT_LOG_INFO("zoo_create ret = "
                       << ret
                       << ", path = "
                       << context->get_request()->path);
  context->set_response(ret);
  return ret;
}

int ZkClient::HandleZkSetReq(ZkSetContextPtr context) {
  int ret = 0;
  if (CheckZk() < 0) {
    ret = ERR_ZK_CONNECTION_LOSS;
    context->set_response(ret);
    return ret;
  }

  ret = zoo_set(m_zh,
                context->get_request()->path.c_str(),
                context->get_request()->buffer.c_str(),
                static_cast<int>(context->get_request()->buffer.size()),
                context->get_request()->version);
  context->set_response(ret);
  CTHRIFT_LOG_INFO("zoo_set ret = "
                       << ret
                       << ", path = "
                       << context->get_request()->path);
  return ret;
}

int ZkClient::HandleZkExistsReq(ZkExistsContextPtr context) {
  int ret = 0;
  if (CheckZk() < 0) {
    ret = ERR_ZK_CONNECTION_LOSS;
    context->set_response(ret);
    return ret;
  }

  struct Stat stat;
  ret = zoo_exists(m_zh,
                   context->get_request()->path.c_str(),
                   context->get_request()->watch,
                   &stat);
  CTHRIFT_LOG_INFO("zoo_exists ret = "
                       << ret
                       << ", path = "
                       << context->get_request()->path);
  context->set_response(ret);
  return ret;
}

int ZkClient::ZkPathCreateRecursivly(std::string zk_path) {
  if (0 != CheckZk()) {
    return ERR_ZK_CONNECTION_LOSS;
  }
  int32_t ret = FAILURE;
  boost::trim(zk_path);
  ZkExistsRequestPtr zk_exists_req(new ZkExistsRequest());
  zk_exists_req->watch = 0;

  ZkCreateRequestPtr zk_data_create(new ZkCreateRequest());
  zk_data_create->path = zk_path;
  zk_data_create->value = "";
  zk_data_create->value_len = 0;
  std::vector<std::string> path_list;
  int32_t path_len = ZkTools::SplitStringIntoVector(zk_path.c_str(), "/",
                                                   path_list);
  if(0 >= path_len || path_len > kMaxZkPathDepth){
    CTHRIFT_LOG_INFO("create path failed, zk_path: "<< zk_path
                                               <<"; path_len:"<<path_len);
    return FAILURE;
  }
  char path_buffer[MAX_BUF_SIZE] = {0};
  int32_t path_buffer_len = MAX_BUF_SIZE;
  int32_t path_pos = 1;
  std::string create_path = "";
  std::string create_path_tmp = "";
  while(path_pos < path_len){
    create_path = create_path_tmp + "/" +  path_list[path_pos];
    zk_exists_req->path = create_path;
    zk_data_create->path = create_path;
    CTHRIFT_LOG_INFO("create_path zk_path: "<< create_path);
    struct Stat stat;
    if(ZNONODE == zoo_exists(m_zh,
                             zk_exists_req->path.c_str(),
                             zk_exists_req->watch,
                             &stat)){

      if(ZOK == zoo_create(m_zh,
                           zk_data_create->path.c_str(),
                           zk_data_create->value.c_str(),
                           zk_data_create->value_len,
                           &ZOO_OPEN_ACL_UNSAFE /* use ACL of parent */,
                           0 /* persistent node*/,
                           path_buffer,
                           path_buffer_len)){
        create_path_tmp = create_path;
        CTHRIFT_LOG_INFO("zoo_create ret = "
                        << ret
                        << ", create_path = "
                        << create_path<<"; create_path_tmp:"<<create_path_tmp);
        path_pos++;
      }else{
        CTHRIFT_LOG_ERROR("zoo_create failed create_path_tmp = "
                            ""<<create_path_tmp);
        return FAILURE;
      }

    }else{
      create_path_tmp = create_path;
      path_pos++;
    }
  }
  return ret;
}

void ZkClient::InitZkGetThreadPool(muduo::net::EventLoop *mns_loop,
                                   int num_threads, const std::string &name) {
  CTHRIFT_LOG_INFO("init the zk get threads num: " << num_threads);
  if (0 == num_threads || num_threads > 2) {
    CTHRIFT_LOG_ERROR("the wrong init client thread num = " << num_threads);
    num_threads = 2;
  }
  zk_get_pool = new muduo::net::EventLoopThreadPool(mns_loop, name);
  zk_get_pool->setThreadNum(num_threads);
  SetThreadInitCallback(boost::bind(&ZkClient::Init, this));
  zk_get_pool->start(threadInitCallback_);
  pool_loops_ = zk_get_pool->getAllLoops();
}

void ZkClient::Init(void) {
  CTHRIFT_LOG_INFO("init(): pid = " << getpid()
                                    << "tid = "
                                    << muduo::CurrentThread::tid());
}
}  // namespace meituan_cthrift