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

#include "cthrift_svr.h"

using namespace std;
using namespace muduo;
using namespace muduo::net;
using namespace meituan_cthrift;

// 5 min stale connect collection
const double CthriftSvr::kTMaxCliIdleTimeSec = 5 * 60.0;
const int8_t CthriftSvr::kI8TimeWheelGridNum = 4;

__thread int32_t CthriftSvr::i32_curr_conn_num_;

__thread boost::shared_ptr<TMemoryBuffer> *
    CthriftSvr::sp_p_input_tmemorybuffer_;
__thread boost::shared_ptr<TMemoryBuffer> *
    CthriftSvr::sp_p_output_tmemorybuffer_;

__thread boost::shared_ptr<TProtocol> *CthriftSvr::sp_p_input_tprotocol_;
__thread boost::shared_ptr<TProtocol> *CthriftSvr::sp_p_output_tprotocol_;

__thread boost::shared_ptr<TProcessor> *CthriftSvr::sp_p_processor_;

void CthriftSvr::InitStaticThreadLocalMember(void) {
  i32_curr_conn_num_ = 0;

  if (CTHRIFT_LIKELY(
      !sp_p_input_tmemorybuffer_ || !(*sp_p_input_tmemorybuffer_))) {
    sp_p_input_tmemorybuffer_ = new boost::shared_ptr<TMemoryBuffer>
        (boost::make_shared<TMemoryBuffer>());
  }

  if (CTHRIFT_LIKELY(
      !sp_p_output_tmemorybuffer_ || !(*sp_p_output_tmemorybuffer_))) {
    sp_p_output_tmemorybuffer_ = new boost::shared_ptr<TMemoryBuffer>
        (boost::make_shared<TMemoryBuffer>());
  }

  if (CTHRIFT_LIKELY(!sp_p_input_tprotocol_ || !(*sp_p_input_tprotocol_))) {
    // inputProtocolFactory_ is member of TServer
    sp_p_input_tprotocol_ =
        new boost::shared_ptr<TProtocol>(
            inputProtocolFactory_->getProtocol(*sp_p_input_tmemorybuffer_));
  }

  if (CTHRIFT_LIKELY(!sp_p_output_tprotocol_ || !(*sp_p_output_tprotocol_))) {
    sp_p_output_tprotocol_ =
        new boost::shared_ptr<TProtocol>(
            inputProtocolFactory_->getProtocol(*sp_p_output_tmemorybuffer_));
  }

  if (CTHRIFT_LIKELY(!sp_p_processor_ || !(*sp_p_processor_))) {
    sp_p_processor_ =
        new boost::shared_ptr<TProcessor>(getProcessor(*sp_p_input_tprotocol_,
                                                       *sp_p_output_tprotocol_,
                                                       boost::make_shared<
                                                           TNullTransport>()));
  }
}

CthriftSvr::~CthriftSvr(void) {
  if (CTHRIFT_LIKELY(sp_p_input_tmemorybuffer_)) {
    delete sp_p_input_tmemorybuffer_;
  }

  if (CTHRIFT_LIKELY(sp_p_output_tmemorybuffer_)) {
    delete sp_p_output_tmemorybuffer_;
  }

  if (CTHRIFT_LIKELY(sp_p_input_tprotocol_)) {
    delete sp_p_input_tprotocol_;
  }

  if (CTHRIFT_LIKELY(sp_p_output_tprotocol_)) {
    delete sp_p_output_tprotocol_;
  }

  if (CTHRIFT_LIKELY(sp_p_processor_)) {
    delete sp_p_processor_;
  }
}

int32_t CthriftSvr::Init(void) {

  int ret = g_cthrift_config.LoadConfig(false);
  if (ret != SUCCESS) {
    CTHRIFT_LOG_ERROR("Fail to load config. errno" <<  ret);
    return ret;
  }

  str_svr_appkey_ = g_cthrift_config.server_appkey_;
  u16_svr_port_ = static_cast<uint16_t >(g_cthrift_config.listen_port_);
  i32_max_conn_num_ = g_cthrift_config.server_max_connnum_;
  i32_svr_overtime_ms_ = g_cthrift_config.server_timeout_;
  i16_conn_thread_num_ =
      static_cast<int16_t >(g_cthrift_config.server_conn_threadnum_);
  i16_worker_thread_num_ =
      static_cast<int16_t >(g_cthrift_config.server_work_threadnum_);
  con_collection_interval_ =
      static_cast<double >(g_cthrift_config.server_conn_gctime_);

  std::string str_reason;
  ret = ArgumentCheck(str_svr_appkey_,
                      u16_svr_port_,
                      i32_svr_overtime_ms_,
                      i32_max_conn_num_,
                      1,
                      &str_reason);
  if (ret != 0) {
    CTHRIFT_LOG_ERROR("ArgumentCheck : " << str_reason << " ret : " << ret );
    return ret;
  }

  try {
    sp_server_ = boost::make_shared<muduo::net::TcpServer>(
        &event_loop_,
        muduo::net::InetAddress(u16_svr_port_),
        str_svr_appkey_ + "_" + "cthrift_svr");
  } catch (const muduo::Exception &ex) {
    CTHRIFT_LOG_ERROR("port: " << u16_svr_port_
                               << " has been occupied by other process.");
    return ERR_PARA_INVALID;
  }

  if (g_cthrift_config.server_register_) {
    int ret = CthriftNameService::InitNS();
    if (0 != ret) {
      CTHRIFT_LOG_ERROR("init zk failed ");
      return ERR_NS_CON_NOT_READY;
    }
  }

  sp_server_->setConnectionCallback(boost::bind(&CthriftSvr::OnConn,
                                                this,
                                                _1));
  sp_server_->setMessageCallback(boost::bind(&CthriftSvr::OnMsg,
                                             this,
                                             _1,
                                             _2,
                                             _3));

  sp_server_->setWriteCompleteCallback(
      boost::bind(&CthriftSvr::OnWriteComplete,
                  this,
                  _1));

  muduo::CountDownLatch countdown_connthread_init(i16_conn_thread_num_);
  // just set, NOT start thread until start()
  sp_server_->setThreadNum(i16_conn_thread_num_);

  // NO InitStaticThreadLocalMember in main thread since NO handle here
  sp_server_->setThreadInitCallback(boost::bind(&CthriftSvr::ConnThreadInit,
                                                this,
                                                &countdown_connthread_init));
  sp_server_->start();

  setInputProtocolFactory(boost::make_shared<CthriftTBinaryProtocolFactory>());
  setOutputProtocolFactory(boost::make_shared<CthriftTBinaryProtocolFactory
  >());

  CthriftNameService::PackDefaultSgservice(str_svr_appkey_,
                                           CthriftNameService::str_local_ip_,
                                           u16_svr_port_,
                                           &sg_service_);

  countdown_connthread_init.wait();
  CTHRIFT_LOG_INFO("conn thread init done");

  // init worker thread
  string str_pool_name("cthrift_svr_worker_event_thread_pool");
  muduo::CountDownLatch countdown_workerthread_init(i16_worker_thread_num_);

  for (int i = 0; i < i16_worker_thread_num_; i++) {
    char buf[str_pool_name.size() + 32];
    snprintf(buf, sizeof buf, "%s%d", str_pool_name.c_str(), i);

    //memory leak， but should use these threads during whole
    // process lifetime, so ignore
    EventLoopThread *p_eventloop_thread = new EventLoopThread(boost::bind(
        &CthriftSvr::WorkerThreadInit,
        this,
        &countdown_workerthread_init),
                                                              buf);

    vec_worker_event_loop_.push_back(p_eventloop_thread->startLoop());
  }

  countdown_workerthread_init.wait();

  CTHRIFT_LOG_INFO("worker thread init done");

  event_loop_.runEvery(SENCOND_COUNT_IN_MIN,
                       boost::bind(&CthriftSvr::StatMsgNumPerMin, this));

  // every 5 min, clear keep-increment worker thread pos, for performance
  event_loop_.runEvery(WORKER_THREAD_POS_SECOND,
                       boost::bind(&CthriftSvr::InitWorkerThreadPos,
                                   this));

  return SUCCESS;
}

int32_t CthriftSvr::ArgumentCheck(const string &str_app_key,
                                 const uint16_t &u16_port,
                                 const int32_t &i32_svr_overtime_ms,
                                 const int32_t &i32_max_conn_num,
                                 const int8_t &i8_check_type,
                                 string *p_str_reason) const {
  std::string str_argument("appkey: " + str_app_key);

  std::string str_port;

  try {
    str_port = boost::lexical_cast<std::string>(u16_port);
  } catch (boost::bad_lexical_cast &e) {
    CTHRIFT_LOG_ERROR("boost::bad_lexical_cast :" << e.what()
                                                  << "u16_port : " << u16_port);

    return ERR_INVALID_PORT;
  }

  str_argument += " port: " + str_port;

  if (1 == i8_check_type) {
    std::string str_svr_overtime_ms;

    try {
      str_svr_overtime_ms =
          boost::lexical_cast<std::string>(i32_svr_overtime_ms);
    } catch (boost::bad_lexical_cast &e) {
      CTHRIFT_LOG_ERROR("boost::bad_lexical_cast :" << e.what()
                                                    << "i32_svr_overtime_ms : "
                                                    << i32_svr_overtime_ms);

      return ERR_INVALID_TIMEOUT;
    }

    str_argument += " svr overtime: " + str_svr_overtime_ms;

    std::string str_max_conn_num;

    try {
      str_max_conn_num = boost::lexical_cast<std::string>(i32_max_conn_num);
    } catch (boost::bad_lexical_cast &e) {
      CTHRIFT_LOG_ERROR("boost::bad_lexical_cast :" << e.what()
                                                    << "i32_max_conn_num : "
                                                    << i32_max_conn_num);

      return ERR_INVALID_MAX_CONNNUM;
    }

    str_argument += " svr max conn num: " + str_max_conn_num;
  }

  CTHRIFT_LOG_DEBUG("argument: " << str_argument);

  if (CTHRIFT_UNLIKELY(str_app_key.empty() || 0 == u16_port || (1 ==
      i8_check_type && (0 > i32_svr_overtime_ms
      // i32_svr_overtime_ms/i32_max_conn_num can be 0, means NO limit
      || 0 > i32_max_conn_num)))) {
    p_str_reason->assign("argument: " + str_argument + ", some or all of "
        "them invalid, please check");
    CTHRIFT_LOG_WARN(*p_str_reason);
    return ERR_PARA_INVALID;
  }

  return SUCCESS;
}

void CthriftSvr::RegSvr(void) {
  try {
    map<string, meituan_mns::ServiceDetail>::iterator it = sg_service_.serviceInfo.begin();
    for (; it != sg_service_.serviceInfo.end(); ++it) {
      CTHRIFT_LOG_DEBUG("servicename :" << it->first);
    }

    int32_t i32_ret = CthriftNameService::RegisterService(sg_service_);
    if (CTHRIFT_UNLIKELY(i32_ret)) {
      CTHRIFT_LOG_ERROR("registService failed: " << i32_ret);
      event_loop_.runAfter(KD_RETRY_INTERVAL_SEC,
                           boost::bind(&CthriftSvr::RegSvr, this));
    } else {
      CTHRIFT_LOG_INFO("reg svr done");
    }
  } catch (TException &tx) {
    CTHRIFT_LOG_ERROR("registService failed: " << tx.what());
    event_loop_.runAfter(KD_RETRY_INTERVAL_SEC,
                         boost::bind(&CthriftSvr::RegSvr, this));
  }
}

void CthriftSvr::serve() {
  if (g_cthrift_config.server_register_) {
    RegSvr();
  }
  event_loop_.loop();
}

void CthriftSvr::stop() {
  // stop data input first
  (sp_server_->getLoop())->runInLoop(boost::bind(&EventLoop::quit,
                                                 sp_server_->getLoop()));

  muduo::CurrentThread::sleepUsec(SLEEP_FOR_SAFE_EXIT);  // for safe

  for (int i = 0; i < i16_worker_thread_num_; i++) {
    vec_worker_event_loop_[i]->quit();
  }  // finish current work

  CthriftNameService::UnInitNS();
  muduo::CurrentThread::sleepUsec(SLEEP_FOR_SAFE_EXIT);  // for safe
}

void
CthriftSvr::OnWriteComplete(const muduo::net::TcpConnectionPtr &conn) {
  CTHRIFT_LOG_DEBUG("OnWriteComplete");
}

void
CthriftSvr::OnConn(const TcpConnectionPtr &conn) {
  CTHRIFT_LOG_INFO(conn->localAddress().toIpPort() << " -> "
                                                   << conn->peerAddress().toIpPort()
                                                   << " is "
                                                   << (conn->connected() ? "UP"
                                                                         : "DOWN")
                                                   << " Name:" << conn->name());

  if (conn->connected()) {
    static int32_t i32_max_conn_num_per_thread =
        static_cast<int32_t>(i32_max_conn_num_ /
            kI16CpuNum);
    if (CTHRIFT_UNLIKELY(i32_curr_conn_num_ >= i32_max_conn_num_per_thread)) {
      CTHRIFT_LOG_WARN("thread max conn " << i32_max_conn_num_per_thread
                                          << " reach");
      conn->forceClose();
      return;
    }

    ++i32_curr_conn_num_;

    ConnEntrySharedPtr sp_conn_entry = boost::make_shared<ConnEntry>(conn);
    (LocalSingConnEntryCirculBuf::instance()).back().insert(sp_conn_entry);

    ConnEntryWeakPtr wp_conn_entry(sp_conn_entry);

    ConnContextSharedPtr sp_conn_info = boost::make_shared<ConnContext>();
    // sp_conn_info->t_conn = time(0);
    sp_conn_info->wp_conn_entry = wp_conn_entry;

    conn->setContext(sp_conn_info);
    conn->setTcpNoDelay(true);
  } else if (0 < i32_curr_conn_num_) {
    --i32_curr_conn_num_;
  }
}

void CthriftSvr::Process(const boost::shared_ptr<muduo::net::Buffer> &sp_buf,
                         boost::weak_ptr<TcpConnection> wp_tcp_conn,
                         Timestamp timestamp_from_recv) {
  // Process(static_cast<int32_t>(sp_buf->readableBytes() - sizeof(int64_t)),
  TcpConnectionPtr shared_conn(wp_tcp_conn.lock());

  if (!(CTHRIFT_LIKELY(shared_conn && shared_conn->connected()))) {
    CTHRIFT_LOG_ERROR("connection broken, discard response pkg ");
    return;
  }

  CTHRIFT_LOG_DEBUG("begin  work process from conn ");

  Process(static_cast<int32_t>(sp_buf->readableBytes()),
          reinterpret_cast<uint8_t *>(const_cast<char *>
          (sp_buf->peek())),
          wp_tcp_conn, timestamp_from_recv);

  CTHRIFT_LOG_DEBUG("end  work process from conn:");
}

void CthriftSvr::Process(const int32_t &i32_req_size,
                         uint8_t *p_ui8_req_buf,
                         boost::weak_ptr<TcpConnection> wp_tcp_conn,
                         Timestamp timestamp_from_recv) {
  TcpConnectionPtr sp_tcp_conn(wp_tcp_conn.lock());
  if (!CTHRIFT_LIKELY(sp_tcp_conn && sp_tcp_conn->connected())) {
    CTHRIFT_LOG_ERROR("connection broken, discard response pkg from conn  ");
    return;
  }

  if (CTHRIFT_UNLIKELY(0 == i32_req_size
                           || 0 == p_ui8_req_buf)) {
    CTHRIFT_LOG_ERROR("i32_req_size "
                          << i32_req_size << "  OR p_ui8_req_buf = NULL ");

    return;
  }

  const double
      d_svr_overtime_secs =
      static_cast<double>(i32_svr_overtime_ms_) / MILLISENCOND_COUNT_IN_SENCOND;

  // check if more than svr overtime
  if (i32_svr_overtime_ms_
      && CheckOverTime(timestamp_from_recv,
                       d_svr_overtime_secs, 0)) {
    CTHRIFT_LOG_WARN("before business handle, already overtime, "
                         "maybe queue congest, drop the request");

    return;
  }

  (*sp_p_input_tmemorybuffer_)->resetBuffer(p_ui8_req_buf,
                                            i32_req_size,
                                            TMemoryBuffer::COPY);

  (*sp_p_output_tmemorybuffer_)->resetBuffer();
  (*sp_p_output_tmemorybuffer_)->getWritePtr(sizeof(int32_t));
  (*sp_p_output_tmemorybuffer_)->wroteBytes(sizeof(int32_t));

  Timestamp timestamp_begin_business = Timestamp::now();
  double d_business_time_diff_ms = 0.0;

  CTHRIFT_LOG_INFO("Begin business Process");
  try {
    (*sp_p_processor_)->process(*sp_p_input_tprotocol_,
                                *sp_p_output_tprotocol_, 0);
  } catch (exception &e) {
    CTHRIFT_LOG_ERROR("exception from business process: " << e.what());

    d_business_time_diff_ms =
        timeDifference(Timestamp::now(), timestamp_begin_business) *
            MILLISENCOND_COUNT_IN_SENCOND;
    CTHRIFT_LOG_DEBUG("business cost " << d_business_time_diff_ms << "ms");

    if (i32_svr_overtime_ms_
        && CheckOverTime(timestamp_from_recv, d_svr_overtime_secs,
                         0)) {
      CTHRIFT_LOG_WARN("after business handle, already overtime "
                           << i32_svr_overtime_ms_ << "ms, business cost "
                           << d_business_time_diff_ms
                           << " ms, just WARN without drop");
    }

    return;
  }
  CTHRIFT_LOG_INFO("End business Process ");

  d_business_time_diff_ms =
      timeDifference(Timestamp::now(), timestamp_begin_business) *
          MILLISENCOND_COUNT_IN_SENCOND;
  CTHRIFT_LOG_DEBUG("business cost " << d_business_time_diff_ms << "ms");

  if (i32_svr_overtime_ms_
      && CheckOverTime(timestamp_from_recv,
                       d_svr_overtime_secs, 0)) {
    CTHRIFT_LOG_WARN("after business handle, already overtime "
                         << i32_svr_overtime_ms_ << "ms, business cost "
                         << d_business_time_diff_ms
                         << " ms, just WARN without drop");
  }

  double total_cost = timeDifference(Timestamp::now(), timestamp_from_recv) *
      MILLISENCOND_COUNT_IN_SENCOND;

  CTHRIFT_LOG_DEBUG("after business process, "
                        "total cost " << total_cost << "ms");

  uint8_t *p_u8_res_buf = 0;
  uint32_t u32_res_size = 0;
  (*sp_p_output_tmemorybuffer_)->getBuffer(&p_u8_res_buf, &u32_res_size);

  if (CTHRIFT_UNLIKELY(sizeof(int32_t) >= u32_res_size)) {
    CTHRIFT_LOG_ERROR("u32_res_size " << u32_res_size << " NOT enough ");

    return;
  }

  int32_t i32_body_size =
      static_cast<int32_t>(htonl(static_cast<uint32_t>(u32_res_size - sizeof
          (int32_t))));
  memcpy(p_u8_res_buf, &i32_body_size, sizeof(int32_t));

  if (CTHRIFT_LIKELY(sp_tcp_conn && sp_tcp_conn->connected())) {
    sp_tcp_conn->send(p_u8_res_buf, u32_res_size);  // already check when begin
  } else {
    CTHRIFT_LOG_WARN("connection broken, discard response pkg ");
  }

  CTHRIFT_LOG_INFO("Process Done from Peer:"
                       << (sp_tcp_conn->peerAddress()).toIpPort());
}

void CthriftSvr::OnMsg(const muduo::net::TcpConnectionPtr &conn,
                       muduo::net::Buffer *buffer,
                       Timestamp receiveTime) {
  CTHRIFT_LOG_DEBUG("OnMsg address  " << (conn->peerAddress()).toIpPort()
                                      << " Current EventLoop size "
                                      << EventLoop::getEventLoopOfCurrentThread()->queueSize());

  if (CTHRIFT_UNLIKELY((conn->getContext()).empty())) {
    CTHRIFT_LOG_ERROR("address: "
                          << (conn->peerAddress()).toIpPort()
                          << " context empty");
    return;
  }

  ConnContextSharedPtr sp_conn_info;
  try {
    sp_conn_info =
        boost::any_cast<ConnContextSharedPtr>(conn->getContext());
  } catch (boost::bad_any_cast &e) {
    CTHRIFT_LOG_ERROR("bad_any_cast:" << e.what());
    return;
  }

  // 先进行可用连接激活操作，避免正常连接被剔除.
  // 放在while循环后，永远无法执行该段逻辑
  sp_conn_info->t_last_active = time(0);

  ConnEntrySharedPtr sp_conn_entry((sp_conn_info->wp_conn_entry).lock());
  if (CTHRIFT_UNLIKELY(!sp_conn_entry)) {
    CTHRIFT_LOG_ERROR("sp_conn_entry invalid??");
    return;
  } else {
    (LocalSingConnEntryCirculBuf::instance()).back().insert(sp_conn_entry);
  }

  bool more = true;
  while (more) {
    if (sp_conn_info->enum_state == kExpectFrameSize) {
      if (sizeof(int32_t) <= buffer->readableBytes()) {
        sp_conn_info->i32_want_size =
            static_cast<uint32_t>(buffer->readInt32());
        sp_conn_info->enum_state = kExpectFrame;
      } else {
        more = false;
      }

    } else if (sp_conn_info->enum_state == kExpectFrame) {
      if (buffer->readableBytes() >=
          static_cast<size_t>(sp_conn_info->i32_want_size)) {
        // stat increment
        atom_i64_recv_msg_per_min_.increment();

        TcpConnWeakPtr wp_tcp_conn(conn);


        /*if (buffer->readableBytes() ==
            static_cast<size_t>(sp_conn_info->i32_want_size)
            || 0 == i16_worker_thread_num_) {*/


        /* if (0 == i16_worker_thread_num_) {


           CTHRIFT_LOG_DEBUG
               << "Read water is low OR single thread, do job by conn thread";

           Process(sp_conn_info->i32_want_size,
                   reinterpret_cast<uint8_t *>((
                       const_cast<char *>(buffer->peek()))),
                   wp_tcp_conn,
                   receiveTime);

           *//*} else if (buffer->readableBytes() > static_cast<size_t>
          (sp_conn_info->i32_want_size)) {*//*


        } else {


          CTHRIFT_LOG_DEBUG << "Read water is high, do job by worker thread";*/

        // use by other thread, so should copy out
        boost::shared_ptr<muduo::net::Buffer> sp_copy_buf =
            boost::make_shared<muduo::net::Buffer>();

        sp_copy_buf->append(reinterpret_cast<uint8_t *>((
                                const_cast<char *>(buffer->peek()))),
                            static_cast<size_t>
                            (sp_conn_info->i32_want_size));

        // pick worker thread, RR
        EventLoop *p_worker_event_loop = 0;
        if (CTHRIFT_LIKELY(1 < i16_worker_thread_num_)) {
          p_worker_event_loop =
              vec_worker_event_loop_[atom_i64_worker_thread_pos_.getAndAdd(1)
                  % i16_worker_thread_num_];
        } else if (CTHRIFT_UNLIKELY(1 == i16_worker_thread_num_)) {
          p_worker_event_loop = vec_worker_event_loop_[0];
        }  // round robin choose next worker thread, no mutex for performance

        CTHRIFT_LOG_DEBUG("io Thread begin into work Thread  "
                              "EventLoop queueSize "
                              << p_worker_event_loop->queueSize());
        p_worker_event_loop->runInLoop(boost::bind(&CthriftSvr::Process,
                                                   this,
                                                   sp_copy_buf,
                                                   wp_tcp_conn,
                                                   receiveTime));

        CTHRIFT_LOG_DEBUG("io Thread end into work Thread");

        buffer->retrieve(static_cast<size_t>(sp_conn_info->i32_want_size));
        sp_conn_info->enum_state = kExpectFrameSize;

        // 收到一个完整的包后，进行可用连接激活操作，避免正常连接被剔除.
        // 放在while循环后，永远无法执行该段逻辑
        sp_conn_info->t_last_active = time(0);

        ConnEntrySharedPtr sp_conn_entry((sp_conn_info->wp_conn_entry).lock());
        if (CTHRIFT_UNLIKELY(!sp_conn_entry)) {
          CTHRIFT_LOG_ERROR("sp_conn_entry invalid??");
          return;
        } else {
          (LocalSingConnEntryCirculBuf::instance()).back().insert(
              sp_conn_entry);
        }

      } else {
        more = false;
      }
    }
  }
}

void CthriftSvr::TimewheelKick(void) {
  (LocalSingConnEntryCirculBuf::instance()).push_back(ConnEntryBucket());
}

void
CthriftSvr::ConnThreadInit(
    muduo::CountDownLatch *p_countdown_connthread_init) {

  InitStaticThreadLocalMember();

  // time wheel
  assert(LocalSingConnEntryCirculBuf::pointer() == NULL);
  LocalSingConnEntryCirculBuf::instance();
  assert(LocalSingConnEntryCirculBuf::pointer() != NULL);
  (LocalSingConnEntryCirculBuf::instance()).resize(kI8TimeWheelGridNum);

  double dLoopInter = (0.0 == con_collection_interval_) ?
                      ((kTMaxCliIdleTimeSec) / kI8TimeWheelGridNum) :
                      (con_collection_interval_ / kI8TimeWheelGridNum);
  CTHRIFT_LOG_DEBUG("dLoopInter " << dLoopInter);

  EventLoop::getEventLoopOfCurrentThread()->runEvery(dLoopInter,
                                                     boost::bind(&CthriftSvr::TimewheelKick,
                                                                 this));

  p_countdown_connthread_init->countDown();
}

void CthriftSvr::WorkerThreadInit(
    muduo::CountDownLatch *p_countdown_workthread_init) {

  InitStaticThreadLocalMember();
  p_countdown_workthread_init->countDown();
}

void CthriftSvr::StatMsgNumPerMin(void) {
  CTHRIFT_LOG_INFO(atom_i64_recv_msg_per_min_.getAndSet(0) /
      SENCOND_COUNT_IN_MIN << " msg per second");
}

// init start pos for avoid big-number-mod performance issue
void CthriftSvr::InitWorkerThreadPos(void) {
  if (CTHRIFT_LIKELY(1 < i16_worker_thread_num_)) {
    CTHRIFT_LOG_DEBUG(atom_i64_worker_thread_pos_.getAndSet(0)
                          << " msg per 5 mins");
  }
}
