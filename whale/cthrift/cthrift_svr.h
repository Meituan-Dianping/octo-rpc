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

#ifndef CTHRIFT_SRC_CTHRIFT_CTHRIFT_SVR_H_
#define CTHRIFT_SRC_CTHRIFT_CTHRIFT_SVR_H_

#include "cthrift/util/cthrift_common.h"
#include "cthrift_client.h"
#include "cthrift_tbinary_protocol.h"
#include "cthrift_transport.h"
#include "cthrift_name_service.h"

namespace meituan_cthrift {
using apache::thrift::server::TServer;
using apache::thrift::TProcessor;
using apache::thrift::TProcessorFactory;
using apache::thrift::protocol::TProtocolFactory;
using apache::thrift::transport::TTransportFactory;

typedef boost::weak_ptr<muduo::net::TcpConnection> TcpConnWeakPtr;

struct ConnEntry : public muduo::copyable {
  TcpConnWeakPtr wp_conn_;

  explicit ConnEntry(const TcpConnWeakPtr &wp_conn)
      : wp_conn_(wp_conn) {}

  ~ConnEntry(void) {
    muduo::net::TcpConnectionPtr sp_conn = wp_conn_.lock();

    if (sp_conn && sp_conn->connected()) {
      CTHRIFT_LOG_INFO("conn " << (sp_conn->peerAddress()).toIpPort()
                               << " timeout");
      sp_conn->shutdown();
    }
  }
};

typedef boost::weak_ptr<ConnEntry> ConnEntryWeakPtr;

struct ConnContext {
 public:
  enum meituan_cthrift::State enum_state;
  int32_t i32_want_size;
  // time_t t_conn;
  time_t t_last_active;
  ConnEntryWeakPtr wp_conn_entry;
  ConnContext(void)
      : enum_state(kExpectFrameSize), i32_want_size(0),
        t_last_active(0) {}
};

typedef boost::shared_ptr<ConnContext> ConnContextSharedPtr;
typedef boost::weak_ptr<ConnContext> ConnContextWeakPtr;

class CthriftSvr : boost::noncopyable,
                   public TServer {
 private:
  // time wheel
  typedef boost::shared_ptr<ConnEntry> ConnEntrySharedPtr;
  // more than one data entry in one grid in circule buffer
  typedef boost::unordered_set<ConnEntrySharedPtr>
      ConnEntryBucket;
  typedef boost::circular_buffer<ConnEntryBucket> ConnEntryBucketCirculBuf;
  typedef muduo::ThreadLocalSingleton <ConnEntryBucketCirculBuf>
      LocalSingConnEntryCirculBuf;        // kick idle conn

  // static const double kDCheckConnIntervalSec;
  static const double kTMaxCliIdleTimeSec;
  static const int8_t kI8TimeWheelGridNum;

  static __thread boost::shared_ptr<TMemoryBuffer>
      *sp_p_input_tmemorybuffer_;
  static __thread boost::shared_ptr<TMemoryBuffer>
      *sp_p_output_tmemorybuffer_;

  static __thread boost::shared_ptr<TProtocol>
      *sp_p_input_tprotocol_;
  static __thread boost::shared_ptr<TProtocol>
      *sp_p_output_tprotocol_;

  static __thread boost::shared_ptr<TProcessor> *sp_p_processor_;

  std::string str_svr_appkey_;
  uint16_t u16_svr_port_;

  int32_t i32_max_conn_num_;
  int32_t i32_svr_overtime_ms_;
  int16_t i16_conn_thread_num_;
  int16_t i16_worker_thread_num_;
  int8_t i8_heartbeat_status_;

  // s秒
  double con_collection_interval_;

  static __thread int32_t i32_curr_conn_num_;

  muduo::net::EventLoop event_loop_;  // guarantee init before server_ !!
  boost::shared_ptr<muduo::net::TcpServer> sp_server_;

  muduo::AtomicInt64 atom_i64_worker_thread_pos_;
  std::vector<muduo::net::EventLoop *> vec_worker_event_loop_;

  meituan_mns::SGService sg_service_;

  boost::shared_ptr<muduo::net::TimerId>
      sp_timerid_regsvr_;  // use to control getsvrlist

  muduo::AtomicInt64 atom_i64_recv_msg_per_min_;

  void OnConn(const muduo::net::TcpConnectionPtr &conn);
  void OnMsg(const muduo::net::TcpConnectionPtr &conn,
             muduo::net::Buffer *buffer,
             muduo::Timestamp receiveTime);
  void OnWriteComplete(const muduo::net::TcpConnectionPtr &conn);

  void WorkerThreadInit(muduo::CountDownLatch *p_countdown_workthread_init);

  void Process(const boost::shared_ptr<muduo::net::Buffer> &sp_buf,
               boost::weak_ptr<muduo::net::TcpConnection> wp_tcp_conn,
               Timestamp timestamp_from_recv);

  void Process(const int32_t &i32_req_size,
               uint8_t *p_u8_req_buf,
               boost::weak_ptr<muduo::net::TcpConnection> wp_tcp_conn,
               muduo::Timestamp timestamp);

  void TimewheelKick(void);

  void ConnThreadInit(muduo::CountDownLatch *p_countdown_connthread_init);

  void RegSvr(void);

  int32_t ArgumentCheck(const std::string &str_app_key,
                       const uint16_t &u16_port,
                       const int32_t &i32_svr_overtime_ms,
                       const int32_t &i32_max_conn_num,
      // 0: ONLY check str_app_key & port  1: full check
                       const int8_t &i8_check_type,
                       std::string *p_str_reason) const;

  void InitStaticThreadLocalMember(void);

 public:
  int32_t Init(void);

  // construct with over_time/max_conn_num set/worker_thread
  template<typename Processor>
  CthriftSvr(const boost::shared_ptr<Processor> &processor,
             THRIFT_OVERLOAD_IF(Processor, TProcessor)) throw(TException)
      :TServer(processor),
       con_collection_interval_(kTMaxCliIdleTimeSec) {
  }

  void StatMsgNumPerMin(void);

  void InitWorkerThreadPos(void);

  ~CthriftSvr(void);

  void serve();

  void stop();

  const muduo::string &name() const {
    return sp_server_->name();
  }
};  // CthriftSvr
}  // namespace meituan_cthrift

#endif  // CTHRIFT_SRC_CTHRIFT_CTHRIFT_SVR_H_
