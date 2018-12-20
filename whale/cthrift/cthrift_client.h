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

#ifndef CTHRIFT_SRC_CTHRIFT_CTHRIFT_CLIENT_H_
#define CTHRIFT_SRC_CTHRIFT_CTHRIFT_CLIENT_H_

#include "cthrift/util/cthrift_common.h"
#include "cthrift_tbinary_protocol.h"
#include "cthrift_transport.h"

namespace meituan_cthrift {
using namespace apache::thrift;
using namespace apache::thrift::protocol;
using namespace apache::thrift::transport;

class CthriftClient {
 private:
  static const int32_t kI32DefaultTimeoutMsForClient;

  std::string str_svr_appkey_;
  std::string str_cli_appkey_;
  int32_t i32_timeout_ms_;   // TODO(specify timeout differ by interface)
  std::string str_serviceName_filter_;
  int32_t i32_port_filter_;
  bool b_parallel_;
  int32_t i32_async_pending_threshold_;
  bool b_async_;

  std::string str_server_ip_;
  int16_t i16_server_port_;
  unsigned int i32_worker_num_;

  boost::shared_ptr<CthriftClientWorker> sp_cthrift_client_worker_;

  static muduo::MutexLock work_resource_lock_;
  typedef boost::weak_ptr<CthriftClientWorker> WorkerWeakPtr;
  static boost::unordered_multimap<string, WorkerWeakPtr> map_appkey_worker_;

  int InitWorker(bool async);

 public:
  CthriftClient(const std::string &str_ip,
                const int16_t &i16_port,
                const int32_t &i32_timeout_ms);

  CthriftClient(const std::string &str_svr_appkey,
                const int32_t &i32_timeout);

  ~CthriftClient(void) {
    // 可以正常释放资源
    // delete will cause memory issue, cthriftclient should keepalive during
    // thread life-time, so two pointers leak acceptable

    /*if (CTHRIFT_LIKELY(p_sp_cthrift_tbinary_protocol_)) {
      delete p_sp_cthrift_tbinary_protocol_;
    }

    if (CTHRIFT_LIKELY(p_sp_cthrift_transport_)) {
      delete p_sp_cthrift_transport_;
    }*/
  }

  boost::shared_ptr<TProtocol> GetCthriftProtocol(void);

  int SetClientAppkey(const std::string &str_appkey);

  int SetFilterPort(const unsigned int &i32_port);

  int SetFilterService(const std::string &str_serviceName);

  inline void SetParallel(const bool &b_par) {
    b_parallel_ = b_par;
  }

  inline void SetThreshold(const int &threshold) {
    i32_async_pending_threshold_ = threshold;
  }

  inline void SetAsync(const bool &b_async) {
    b_async_ = b_async;
  }

  inline void SetWorkerNumber(const unsigned int &num) {
    i32_worker_num_ = num;
  }

  inline int32_t GetThreshold() const {
    return i32_async_pending_threshold_;
  }

  int Init(void);

  int32_t GetTimeout() const {
    return i32_timeout_ms_;
  }

  boost::shared_ptr<CthriftClientWorker> GetClientWorker() {
    return sp_cthrift_client_worker_;
  }

  std::string GetEnvInfo(void) const {
    return meituan_cthrift::CthriftNameService::str_env_;
  }
};

}  // namespace meituan_cthrift

#endif  // CTHRIFT_SRC_CTHRIFT_CTHRIFT_CLIENT_H_
