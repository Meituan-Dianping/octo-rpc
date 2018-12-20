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


#ifndef OCTO_OPEN_SOURCE_MNS_WORKER_H_
#define OCTO_OPEN_SOURCE_MNS_WORKER_H_

#include "thrift_client.h"

namespace mns_sdk {

class ServerListParams {
 public:
  ServerListParams(const meituan_mns::ProtocolResponse &rsp,
                   const meituan_mns::ProtocolRequest &request) :
      ret_(-1),
      timeout_(false),
      rsp_(rsp),
      resuest_(request),
      cond_ready_read_(mutexlock_conn_ready_) {}

  int ret_;
  bool timeout_;
  meituan_mns::ProtocolResponse rsp_;
  meituan_mns::ProtocolRequest resuest_;
  muduo::MutexLock mutexlock_conn_ready_;
  muduo::Condition cond_ready_read_;
};

class RegisterParams {
 public:
  RegisterParams(const meituan_mns::SGService &sgService) :
      ret_(-1),
      timeout_(false),
      sgService_(sgService),
      cond_ready_read_(mutexlock_conn_ready_) {
  }

  int ret_;
  bool timeout_;
  meituan_mns::SGService sgService_;
  muduo::MutexLock mutexlock_conn_ready_;
  muduo::Condition cond_ready_read_;
};

typedef boost::shared_ptr<ServerListParams> ServerListParamsPtr;
typedef boost::shared_ptr<RegisterParams> RegisterParamsPtr;

struct WeightSort {
  bool operator()(const double &a, const double &b) const {
    return a > b;
  }
};

typedef boost::shared_ptr<muduo::net::TcpClient> TcpClientSharedPtr;
typedef boost::weak_ptr<muduo::net::TcpClient> TcpClientWeakPtr;

class ConnInfo {
 private:
  meituan_mns::SGService sgservice_;
  TcpClientSharedPtr sp_tcpclient_;

  std::multimap<double,
                TcpClientWeakPtr,
                WeightSort> *p_map_weight_tcpclientwp_;

//for relate to weight index, if not be deleted, safe
  std::multimap<double, TcpClientWeakPtr>::iterator
      it_map_weight_tcpclientwp_index_;

 public:
  ConnInfo(const meituan_mns::SGService &sgservice_tmp,
           std::multimap<double,
                         TcpClientWeakPtr,
                         WeightSort> *
           p_map_weight_tcpclientwp) : sgservice_(sgservice_tmp),
                                       p_map_weight_tcpclientwp_(
                                           p_map_weight_tcpclientwp) {}

  ~ConnInfo(void) {
    if (MNS_UNLIKELY(!p_map_weight_tcpclientwp_)) {
      MNS_LOG_ERROR("p_map_weight_tcpclientwp_ NULL");
    } else {
      MNS_LOG_INFO("delete appkey " << sgservice_.appkey << " ip: "
                                    << sgservice_.ip << " port: "
                                    << sgservice_.port
                                    << " from weight pool");
      p_map_weight_tcpclientwp_->erase(it_map_weight_tcpclientwp_index_);
    }
  }

  const meituan_mns::SGService &GetSgservice(void) const {
    return sgservice_;
  }

  bool CheckConnHealthy(void) const;

  void UptSgservice(const meituan_mns::SGService &sgservice);

  void setSp_tcpclient_(const TcpClientSharedPtr &sp_tcpclient);

  TcpClientSharedPtr &getSp_tcpclient_() {
    return sp_tcpclient_;
  }

  /*int8_t FetchTcpConnSP(muduo::net::TcpConnectionPtr *p_tcpconn_sp) {
    if (sp_tcpclient_.get() && sp_tcpclient_->connection()) {
      p_tcpconn_sp->reset((sp_tcpclient_->connection()).get());
      return 0;
    }

    LOG_INFO << "sp_tcpclient_ NOT init";
    return -1;
  }*/
};

typedef boost::shared_ptr<ConnInfo> ConnInfoSharedPtr;
typedef boost::weak_ptr<ConnInfo> ConnInfoWeakPtr;

struct ConnContext4Worker {
 public:

  bool b_highwater;
  bool b_occupied;

  ConnInfoWeakPtr wp_conn_info;

  ConnContext4Worker(const ConnInfoSharedPtr &sp_conn_info)
      : b_highwater(false),
        b_occupied(false), wp_conn_info(sp_conn_info) {}
};

typedef boost::shared_ptr<ConnContext4Worker> Context4WorkerSharedPtr;

class MnsWorker {
 private:
  struct CnxtEntry : public muduo::copyable {
    MnsWorker *p_worker_;

    explicit CnxtEntry(MnsWorker *parent)
        : p_worker_(parent) {}

    ~CnxtEntry(void) {
      p_worker_ = NULL;
    }
  };

  typedef boost::unordered_map<std::string, ConnInfoSharedPtr>::iterator
      UnorderedMapStr2SpConnInfoIter;

  boost::shared_ptr<muduo::net::TcpClient> sp_tcpclient_sentinel_;

  muduo::MutexLock mutexlock_avaliable_conn_ready_;
  muduo::Condition cond_avaliable_conn_ready_;

  muduo::AtomicInt32
      atomic_avaliable_conn_num_;  //exclude disconn/highwater/occupied

  int8_t i8_destructor_flag_;

  char *unzip_buf_;

  muduo::net::InetAddress sentinel_url_addr_;

  boost::unordered_map<std::string, meituan_mns::SGService>
      map_ipport_sgservice_; //use compare and update svrlist

  boost::shared_ptr<meituan_mns::ServiceAgentClient> sp_sgagent_client_;

  void OnConn4Sentinel(const muduo::net::TcpConnectionPtr &conn);
  void OnMsg4Sentinel(const muduo::net::TcpConnectionPtr &conn,
                      muduo::net::Buffer *buffer,
                      muduo::Timestamp receiveTime);

  //for common srvlist update
  boost::unordered_map<std::string, ConnInfoSharedPtr>
      map_ipport_spconninfo_;

  std::multimap<double, TcpClientWeakPtr, WeightSort>
      *  //ONLY used by conninfo
      p_multimap_weight_wptcpcli_;

  typedef boost::unordered_map<std::string, ConnInfoSharedPtr>
  ::iterator
      UnorderedMapIpPort2ConnInfoSP;

  typedef std::multimap<double, TcpClientWeakPtr, WeightSort>::iterator
      MultiMapIter;

  boost::shared_ptr<muduo::net::EventLoopThread> sp_event_thread_;

  muduo::net::EventLoop *p_event_loop_;

  void AddSrv(const std::vector<meituan_mns::SGService> &vec_add_sgservice);
  void DelSrv(const std::vector<meituan_mns::SGService> &vec_add_sgservice);
  void ChgSrv(const std::vector<meituan_mns::SGService> &vec_add_sgservice);

  void OnConn(const muduo::net::TcpConnectionPtr &conn);
  void OnMsg(const muduo::net::TcpConnectionPtr &conn,
             muduo::net::Buffer *buffer,
             muduo::Timestamp receiveTime);

  void UpdateSvrList
      (const std::vector<meituan_mns::SGService> &vec_sgservice);

  void InitWorker(void);
  void InitSentinel(void);
  void UnInitSentinel(void);

  void CheckLocalSgagent();
  bool CheckLocalSgagentHealth(void);

  int8_t ChooseNextReadyConn(TcpClientWeakPtr *p_wp_tcpcli);
  static int8_t CheckRegion(const double &d_weight);

 public:
  MnsWorker();

  void ClearTcpClient(muduo::CountDownLatch *p_clear_countdown) {
    MNS_LOG_DEBUG("into worker thread clean resource");
    map_ipport_spconninfo_.clear(); //clear tcpclient, OR may core when
    // multiple tcpclient quit
    if (sp_tcpclient_sentinel_) {
      sp_tcpclient_sentinel_->disconnect();
      sp_tcpclient_sentinel_.reset();
    }

    //p_multimap_weight_wptcpcli_ only need free memory in work thread.
    delete p_multimap_weight_wptcpcli_;
    p_clear_countdown->countDown();
  }

  virtual ~MnsWorker() {
    //set destructor flag
    i8_destructor_flag_ = 1;

    muduo::CountDownLatch clear_countdown(1);
    p_event_loop_->runInLoop(boost::bind(&MnsWorker::ClearTcpClient,
                                         this, &clear_countdown));
    //wait until end of the destructor
    clear_countdown.wait();

    if (MNS_LIKELY(unzip_buf_)) {
      delete unzip_buf_;
    }
  }

  int32_t GetServiceList(meituan_mns::ProtocolResponse &rsp,
                         const meituan_mns::ProtocolRequest &request);

  int32_t RegistService(const meituan_mns::SGService &sgService);

  void NotifyRegistService(RegisterParamsPtr params);

  void NotifyGetServiceList(ServerListParamsPtr params);

  void OnGetServiceList(ServerListParamsPtr params);

  void OnRegistService(RegisterParamsPtr params);

  muduo::net::EventLoop *getP_event_loop_(void) const {
    return p_event_loop_;
  }

  TcpClientSharedPtr GetAvailableConn();

  int32_t getAtomic_avaliable_conn_num_() {
    return atomic_avaliable_conn_num_.get();
  }

  muduo::Condition &getCond_avaliable_conn_ready_() {
    return cond_avaliable_conn_ready_;
  }

  muduo::MutexLock &getMutexlock_avaliable_conn_ready_(void) {
    return mutexlock_avaliable_conn_ready_;
  }

  ThriftClientHandler *InitHandler(const std::string &ip, const int port);

  void Init();
};
}

#endif //OCTO_OPEN_SOURCE_MNS_WORKER_H_
