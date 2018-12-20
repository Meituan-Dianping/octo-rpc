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

#ifndef CTHRIFT_SRC_CTHRIFT_CTHRIFT_CLIENT_WORKER_H_
#define CTHRIFT_SRC_CTHRIFT_CTHRIFT_CLIENT_WORKER_H_

#include "cthrift/util/cthrift_common.h"
#include "cthrift_name_service.h"
#include "cthrift_client.h"
#include "cthrift_transport.h"

namespace meituan_cthrift {
using namespace apache::thrift;
using namespace apache::thrift::protocol;
using namespace apache::thrift::transport;

struct WeightSort {
  bool operator()(const double &a, const double &b) const {
    return a > b;
  }
};

class ConnInfo {
 private:
  meituan_mns::SGService sgservice_;
  TcpClientSharedPtr sp_tcpclient_;

  std::multimap<double,
                TcpClientWeakPtr,
                WeightSort> *p_map_weight_tcpclientwp_;

// for relate to weight index, if not be deleted, safe
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
    if (CTHRIFT_UNLIKELY(!p_map_weight_tcpclientwp_)) {
      CTHRIFT_LOG_ERROR("p_map_weight_tcpclientwp_ NULL");
    } else {
      CTHRIFT_LOG_INFO("delete appkey " << sgservice_.appkey << " ip: "
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

  /*
    int8_t FetchTcpConnSP(muduo::net::TcpConnectionPtr *p_tcpconn_sp) {
    if (sp_tcpclient_.get() && sp_tcpclient_->connection()) {
      p_tcpconn_sp->reset((sp_tcpclient_->connection()).get());
      return 0;
    }

    CTHRIFT_LOG_INFO << "sp_tcpclient_ NOT init";
    return -1;
   }
   */
};

typedef boost::shared_ptr<ConnInfo> ConnInfoSharedPtr;
typedef boost::weak_ptr<ConnInfo> ConnInfoWeakPtr;

struct ConnContext4Worker {
 public:
  State enum_state;
  int32_t i32_want_size;

  /*
   time_t t_last_conn_time_;
   time_t t_last_recv_time_;
   time_t t_last_send_time_;
   */
  bool b_highwater;
  bool b_occupied;

  ConnInfoWeakPtr wp_conn_info;
  // std::queue <std::string> queue_send;

  explicit ConnContext4Worker(const ConnInfoSharedPtr &sp_conn_info)
      : enum_state(kExpectFrameSize), i32_want_size(0), b_highwater(false),
        b_occupied(false), wp_conn_info(sp_conn_info) {}
};

typedef boost::shared_ptr<ConnContext4Worker> Context4WorkerSharedPtr;

class CthriftClientWorker {
 private:
  struct CnxtEntry : public muduo::copyable {
    WeakContSharedPtr wp_cnxt_;
    CthriftClientWorker *p_worker_;

    explicit CnxtEntry(const WeakContSharedPtr &wp_cnxt,
                       CthriftClientWorker *parent)
        : wp_cnxt_(wp_cnxt), p_worker_(parent) {}

    ~CnxtEntry(void) {
      SharedContSharedPtr sp_cnxt = wp_cnxt_.lock();
      if (sp_cnxt && p_worker_) {
        p_worker_->map_id_sharedcontextsp_.erase(sp_cnxt->str_id);
      }
      p_worker_ = NULL;
    }
  };

  typedef boost::shared_ptr<CnxtEntry> AsyncCnxtEntry;
  typedef boost::unordered_set<AsyncCnxtEntry> CnxtEntryBucket;
  typedef boost::circular_buffer<CnxtEntryBucket> CnxtBuf;
  CnxtBuf cnxt_entry_circul_buf_;

  static const int8_t kI8TimeWheelNum;

  typedef boost::unordered_map<std::string, ConnInfoSharedPtr>::iterator
      UnorderedMapStr2SpConnInfoIter;

  static const int32_t kI32HighWaterSize;  // 64K

  muduo::MutexLock mutexlock_avaliable_conn_ready_;
  muduo::Condition cond_avaliable_conn_ready_;

  muduo::AtomicInt32
      atomic_avaliable_conn_num_;  // exclude disconn/highwater/occupied

  std::string str_svr_appkey_;
  std::string str_client_appkey_;
  std::string str_serviceName_filter_;
  int32_t i32_port_filter_;
  int32_t i32_timeout_;

  int8_t i8_destructor_flag_;

  std::string str_server_ip_;
  int16_t i16_server_port_;
  bool b_user_set_ip;

  boost::unordered_map<std::string, meituan_mns::SGService>
      map_ipport_sgservice_;  // use compare and update svrlist

  // boost::shared_ptr<CthriftTransport> sp_cthrift_transport_sgagent_;
  boost::shared_ptr<CthriftClient> sp_cthrift_client_;
  // boost::shared_ptr <SGAgentClient> sp_sgagent_client_;

  // for get seqid from raw buffer
  boost::shared_ptr<TMemoryBuffer>
      *sp_p_tmemorybuffer_;
  boost::shared_ptr<CthriftTBinaryProtocolWithTMemoryBuf>
      *sp_p_cthrift_tbinary_protocol_;

  // for common srvlist update
  boost::unordered_map<std::string, ConnInfoSharedPtr>
      map_ipport_spconninfo_;

  std::multimap<double, TcpClientWeakPtr, WeightSort>
      *  // ONLY used by conninfo
      p_multimap_weight_wptcpcli_;

  typedef boost::unordered_map<std::string, ConnInfoSharedPtr>
  ::iterator
      UnorderedMapIpPort2ConnInfoSP;

  typedef std::multimap<double, TcpClientWeakPtr, WeightSort>::iterator
      MultiMapIter;

  // MultiMapIter it_last_choose_conn;

  boost::shared_ptr<muduo::net::EventLoopThread> sp_event_thread_;

  boost::shared_ptr<muduo::net::EventLoopThread> sp_event_thread_sgagent_;

  muduo::net::EventLoop *p_event_loop_;
  muduo::net::EventLoop *p_event_loop_sgagent_;

  // muduo::net::EventLoopThread *p_async_event_thread_;
  boost::shared_ptr<muduo::net::EventLoopThread> sp_async_event_thread_;
  muduo::net::EventLoop *p_async_event_loop_;

  boost::unordered_map<std::string, SharedContSharedPtr>
      map_id_sharedcontextsp_;

  typedef boost::unordered_map<std::string, SharedContSharedPtr>::iterator
      MapID2SharedPointerIter;

  void AddSrv(
      const std::vector<meituan_mns::SGService> &vec_add_sgservice);
  void DelSrv(
      const std::vector<meituan_mns::SGService> &vec_add_sgservice);
  void ChgSrv(
      const std::vector<meituan_mns::SGService> &vec_add_sgservice);

  void OnConn(const muduo::net::TcpConnectionPtr &conn);
  void OnMsg(const muduo::net::TcpConnectionPtr &conn,
             muduo::net::Buffer *buffer,
             muduo::Timestamp receiveTime);
  void HandleMsg(const muduo::net::TcpConnectionPtr &conn,
                 Context4WorkerSharedPtr &sp_context_worker,
                 muduo::net::Buffer *buffer);

  void HandleThriftMsg(const muduo::net::TcpConnectionPtr &conn,
                       const int32_t &length, uint8_t *buf);

  void OnWriteComplete(const muduo::net::TcpConnectionPtr &conn);
  void OnHighWaterMark(const muduo::net::TcpConnectionPtr &conn, size_t len);

  void UpdateSvrList
      (const std::vector<meituan_mns::SGService> &vec_sgservice);

  void InitWorker(void);
  void InitSgagentHandlerThread(void);

  void GetSvrList(void);

  int8_t ChooseNextReadyConn(TcpClientWeakPtr *p_wp_tcpcli);
  static int8_t CheckRegion(const double &d_weight);

  // filter
  bool FilterAll(const meituan_mns::SGService &sg);
  bool FilterService(const meituan_mns::SGService &sg);
  bool FilterPort(const meituan_mns::SGService &sg);

  // async
  void AsyncCallback(const uint32_t &size,
                     uint8_t *recv_buf,
                     SharedContSharedPtr sp_shared);

 public:
  CthriftClientWorker(const std::string &str_svr_appkey,
                      const std::string &str_cli_appkey,
                      const std::string &str_serviceName_filter,
                      const int32_t &i32_port_filter,
                      const std::string &str_server_ip,
                      const int16_t &i16_server_port);

  void ClearTcpClient(muduo::CountDownLatch *p_clear_countdown) {
    CTHRIFT_LOG_DEBUG("into worker thread clean resource");
    map_ipport_spconninfo_.clear();  // clear tcpclient, OR may core when

    // p_multimap_weight_wptcpcli_ only need free memory in work thread.
    delete p_multimap_weight_wptcpcli_;
    p_clear_countdown->countDown();
  }

  virtual ~CthriftClientWorker() {
    // set destructor flag
    i8_destructor_flag_ = 1;

    muduo::CountDownLatch clear_countdown(1);
    p_event_loop_->runInLoop(boost::bind(&CthriftClientWorker::ClearTcpClient,
                                         this, &clear_countdown));
    // wait until end of the destructor
    clear_countdown.wait();

    if (p_async_event_loop_) {
      p_async_event_loop_->quit();
      sp_async_event_thread_.reset();
    }

    // delete will cause memory issue, CthriftClientWorker should keepalive
    // during
    // thread life-time, so two pointers leak acceptable
    // fix以上问题，可以正常释放
    if (CTHRIFT_LIKELY(sp_p_tmemorybuffer_)) {
      delete sp_p_tmemorybuffer_;
    }

    if (CTHRIFT_LIKELY(sp_p_cthrift_tbinary_protocol_)) {
      delete sp_p_cthrift_tbinary_protocol_;
    }
  }

  muduo::net::EventLoop *getP_event_loop_(void) const {
    return p_event_loop_;
  }

  void DelContextMapByID(const std::string &str_id) {
    MapID2SharedPointerIter
        map_iter = map_id_sharedcontextsp_.find(str_id);
    if (map_iter != map_id_sharedcontextsp_.end()) {
      boost::shared_ptr<muduo::net::TcpConnection>
          sp_send_conn((map_iter->second->wp_send_conn).lock());
      if (sp_send_conn) {
        CTHRIFT_LOG_WARN("del id from ipport :"
                             << (sp_send_conn->peerAddress()).toIpPort());
      }
    }

    CTHRIFT_LOG_WARN("del id " << str_id << " by transport for timeout");
    map_id_sharedcontextsp_.erase(str_id);
  }

  void SendTransportReq(SharedContSharedPtr sp_shared);

  void TimewheelKick();
  void EnableAsync(const int32_t &i32_timeout_ms);
  void AsyncSendReq(SharedContSharedPtr sp_shared);

  int32_t atomic_avaliable_conn_num() {
    return atomic_avaliable_conn_num_.get();
  }

  muduo::Condition &cond_avaliable_conn_ready() {
    return cond_avaliable_conn_ready_;
  }

  muduo::MutexLock &mutexlock_avaliable_conn_ready(void) {
    return mutexlock_avaliable_conn_ready_;
  }
};
}  // namespace meituan_cthrift

#endif  // CTHRIFT_SRC_CTHRIFT_CTHRIFT_CLIENT_WORKER_H_
