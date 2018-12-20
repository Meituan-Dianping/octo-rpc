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

#include "cthrift_zk_client.h"
#include "cthrift_ns_imp.h"

using namespace meituan_cthrift;

const int kPoolWather = 0;
const int kMaxThreadPool = 3;

muduo::net::EventLoopThread ServiceZkClient::s_watcher_thread;
std::vector<muduo::net::EventLoop *> ServiceZkClient::pool_watcher;
muduo::net::EventLoopThreadPool *ServiceZkClient::mns_zk_pool[kMaxThreadPool]
    = {NULL};
muduo::net::EventLoop *ServiceZkClient::mns_loop_pool[kMaxThreadPool] = {NULL};

ServiceZkClient::ServiceZkClient(CthriftNsImp *p) {
  plugin = p;
  MnsThreadPoolInit();
}

void ServiceZkClient::MnsThreadPoolInit() {
  // todo:将函数merge
  mns_loop_pool[kPoolWather] = s_watcher_thread.startLoop();
  mns_loop_pool[kPoolWather]->runInLoop(
      boost::bind(&ServiceZkClient::InitThreadPool,
                  this,
                  mns_loop_pool[kPoolWather],
                  1,
                  "w-trigger "));
}

ServiceZkClient::~ServiceZkClient() {

  for (int i = 0; i < kMaxThreadPool; ++i) {
    SAFE_DELETE(mns_zk_pool[i]);
  }
}

/**
 *
 * @param srvlist - if SUCCESS, srv_list will be updated
 * @param localAppkey
 * @param appKey
 * @param protocol
 * @param is_watcher_callback
 * @return
 */
int ServiceZkClient::GetSrvListByProtocol(
    std::vector<meituan_mns::SGService> *srvlist,
    const std::string &local_appkey,
    const std::string &remote_appkey,
    const std::string &protocol,
    bool is_watcher_callback) {
  // clear the srv_list
  srvlist->clear();

  // generate the zk path of provider
  char provider_path[MAX_BUF_SIZE] = {0};
  std::string node_type = "provider";
  int ret = ZkTools::GenProtocolZkPath(provider_path, remote_appkey,
                                       protocol, node_type);
  if (MNS_SUCCESS != ret) {
    // already log genProtocolZkPath
    return ret;
  }

  std::string provider_path_str = provider_path;
  // wget the data of provider path
  ZkWGetInvokeParams zk_wget_params;

  zk_wget_params.zk_wget_request.path = provider_path_str;
  zk_wget_params.zk_wget_request.watch = ServiceByProtocolWatcher;
  zk_wget_params.zk_wget_request.watcherCtx = plugin;

  ret = ZkTools::InvokeService(meituan_cthrift::ZK_WGET, &zk_wget_params);
  if (ZOK != ret) {
    CTHRIFT_LOG_ERROR("fail to zk wget. ret = "
                          << ret << ", path = " << provider_path);
    return ret;
  }

  struct Stat stat = zk_wget_params.zk_wget_response.stat;

  // get data from zk.
  ZkWGetChildrenInvokeParams wg_child_params;
  wg_child_params.zk_wgetchildren_request.path = provider_path_str;

  wg_child_params.zk_wgetchildren_request.watch = ServiceByProtocolWatcher;
  wg_child_params.zk_wgetchildren_request.watcherCtx = plugin;

  ret = ZkTools::InvokeService(meituan_cthrift::ZK_WGET_CHILDREN,
                               &wg_child_params);
  if (ZOK != ret) {
    CTHRIFT_LOG_ERROR("fail to get children nodes from zk. ret = "
                          << ret << ", path = " << provider_path);
    return ret;
  }

  boost::shared_ptr<std::pair<bool, std::vector<meituan_mns::SGService
  > > >
      service(new std::pair<bool, std::vector<meituan_mns::SGService> >
                  (true, std::vector<meituan_mns::SGService>()));

  GetSubSrvListFromZk(&service, &wg_child_params, 0,
                      provider_path_str, remote_appkey);

  srvlist->insert(srvlist->begin(),
                  service->second.begin(),
                  service->second.end());

  CTHRIFT_LOG_INFO("normal get svrlist from the zk, "
                       "size is " << srvlist->size());


  // 服务节点下线或者反序列化失败
  if (static_cast<size_t>(wg_child_params.zk_wgetchildren_response.count) !=
      srvlist->size()) {
    CTHRIFT_LOG_WARN("srvlist size is " << srvlist->size()
                                        << ", childnode num is "
                                        << wg_child_params.zk_wgetchildren_response.count
                                        << ". Json failed or nodes have been deleted.");
  }
  return MNS_SUCCESS;
}

void ServiceZkClient::ServiceByProtocolWatcher(zhandle_t *zh, int type,
                                               int state, const char *path,
                                               void *watcherCtx) {
  // ZK watcher触发，获取新的服务列表，并更新消息队列
  CTHRIFT_LOG_INFO("rcv the watcher from the ZK server by protocol,path"
                       << path << "type" << type);
  static int round_index = 0;
  if (strlen(path) == 0 || type == -1) {
    CTHRIFT_LOG_ERROR("get event serviceByProtocolWatcher, "
                          " that ZK server may down! state = " << state
                                                               << ", type = "
                                                               << type
                                                               << ", path = "
                                                               << path);
    return;
  } else {
    std::string path_str(path);
    CTHRIFT_LOG_INFO("zk watch trigger: path = " << path_str);
  }
  timeval now_time;
  gettimeofday(&now_time, NULL);
  int64_t rcv_watcher_time = now_time.tv_sec * 1000 + now_time.tv_usec / 1000;
  // Extract appkey from zkpath
  std::string appkey = "";
  std::string protocol = "";
  int ret = ZkTools::DeGenZkPath(path, &appkey, &protocol);
  if (MNS_SUCCESS != ret) {
    CTHRIFT_LOG_ERROR("DeGenZkPath is serviceByProtocolWatcher is wrong!"
                          " path:" << path
                                   << ", appkey:" << appkey
                                   << ", protocol:" << protocol);
    return;
  }
  ServicePtr resServicePtr(new meituan_mns::getservice_res_param_t());
  resServicePtr->__set_localAppkey("sg_agent_protocol_watcher");
  resServicePtr->__set_remoteAppkey(appkey);
  resServicePtr->__set_version("");
  resServicePtr->__set_protocol(protocol);
  CthriftNsImp *p = static_cast<CthriftNsImp *>(watcherCtx);

  CTHRIFT_LOG_INFO("zk watch trigger: "
                       << "appkey = " << resServicePtr->remoteAppkey
                       << " protocol = " << resServicePtr->protocol
                       << " queue size = "
                       << pool_watcher[round_index]->queueSize()
                       << " round_Index" << round_index);

  pool_watcher[round_index]->runInLoop(
      boost::bind(&CthriftNsImp::GetSrvList, p,
                  resServicePtr, rcv_watcher_time));
}

void ServiceZkClient::InitThreadPool(muduo::net::EventLoop *mns_loop,
                                     int num_threads, const std::string &name) {
  if (MIX_ZK_THREADPOOL_NUM > num_threads ||
      num_threads > MAX_ZK_THREADPOOL_NUM) {
    CTHRIFT_LOG_WARN("illeagal watcher threads num: " << num_threads);
  }
  mns_zk_pool[kPoolWather] = new muduo::net::EventLoopThreadPool(mns_loop,
                                                                 name);
  mns_zk_pool[kPoolWather]->setThreadNum(num_threads);
  SetThreadInitCallback(boost::bind(&ServiceZkClient::Init, this));
  mns_zk_pool[kPoolWather]->start(threadInitCallback_);
  pool_watcher = mns_zk_pool[kPoolWather]->getAllLoops();
}

void ServiceZkClient::Init(void) {
  CTHRIFT_LOG_INFO("init(): pid = "
                       << getpid() << ", tid = "
                       << muduo::CurrentThread::tid());
}

int ServiceZkClient::GetSubSrvListFromZk(
    boost::shared_ptr<std::pair<bool,
                                std::vector<meituan_mns::SGService> > > *srvlist,
    ZkWGetChildrenInvokeParams *wg_child_params, const int &index,
    const std::string &provider_path, const std::string &remote_appkey) {
  int ret = FAILURE;
  (*srvlist)->second.clear();
  int begin = 0, end = wg_child_params->zk_wgetchildren_response.count;
  CTHRIFT_LOG_DEBUG("the remote appkey is " << remote_appkey);

  for (int i = begin; i < end; ++i) {
    std::string zk_node_path = provider_path + "/" +
        wg_child_params->zk_wgetchildren_response.data[i];
    // 子节点不加watcher, 减少watcher数量
    ZkGetInvokeParams param;
    param.zk_get_request.
        path = zk_node_path;
    param.zk_get_request.
        watch = 0;
    param.zk_get_request.
        zk_index = index;
    ret = ZkTools::InvokeService(meituan_cthrift::ZK_GET, &param);

    if (ERR_NODE_NOTFIND == ret) {
      CTHRIFT_LOG_WARN(
          "the service node has been deleted, ignore it and continue "
              "updating service list. path = " << zk_node_path);
      continue;
    } else if (ZOK != ret) {
      CTHRIFT_LOG_ERROR("fail to get zk data. ret = " << ret
                                                      << ", path = : "
                                                      << zk_node_path);
      (*srvlist)->first = false;
      return ret;
    }

    std::string node_json_str = param.zk_get_response.buffer;
    CTHRIFT_LOG_DEBUG("succeed to zoo_get, json: " << node_json_str);
    meituan_mns::SGService oservice;
    ret = ZkTools::Json2SGService(node_json_str, &oservice);
    if (ret != 0) {
      CTHRIFT_LOG_WARN("fail to parse node json str. "
                           << ", ret = " << ret
                           << ", path = " << zk_node_path
                           << ", json = " << node_json_str);
      continue;
    }

    // double check
    if (oservice.appkey != remote_appkey) {
      CTHRIFT_LOG_WARN("expected appkey: " << remote_appkey
                                           << ", but node.appky = "
                                           << oservice.appkey
                                           << ", path = " << zk_node_path);
      continue;
    }

    (*srvlist)->second.push_back(oservice);
  }

  CTHRIFT_LOG_DEBUG("the servicelist is " << (*srvlist)->second.size());
  return ret;
}

