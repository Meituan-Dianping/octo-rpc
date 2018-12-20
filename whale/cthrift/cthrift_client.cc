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


#include "cthrift_client_worker.h"

using namespace meituan_cthrift;

const int32_t CthriftClient::kI32DefaultTimeoutMsForClient = 5000;  // 5s
muduo::MutexLock CthriftClient::work_resource_lock_;
boost::unordered_multimap<string, CthriftClient::WorkerWeakPtr>
    CthriftClient::map_appkey_worker_;

CthriftClient::CthriftClient(const std::string &str_svr_appkey,
                             const int32_t &i32_timeout_ms)
    : str_svr_appkey_(str_svr_appkey),
      str_cli_appkey_(""),
      i32_timeout_ms_(
          0 > i32_timeout_ms ? kI32DefaultTimeoutMsForClient : i32_timeout_ms),
      str_serviceName_filter_(""),
      i32_port_filter_(-1),
      b_parallel_(false),
      i32_async_pending_threshold_(DEFAULT_ASYNC_PENDING_THRESHOLD),
      b_async_(false),
      str_server_ip_(""),
      i16_server_port_(0),
      i32_worker_num_(1) {
}

CthriftClient::CthriftClient(const std::string &str_ip,
                             const int16_t &i16_port,
                             const int32_t &i32_timeout_ms)
    : str_svr_appkey_(""),
      str_cli_appkey_(""),
      i32_timeout_ms_(
          0 > i32_timeout_ms ? kI32DefaultTimeoutMS : i32_timeout_ms),
      str_serviceName_filter_(""),
      i32_port_filter_(-1),
      b_parallel_(false),
      i32_async_pending_threshold_(DEFAULT_ASYNC_PENDING_THRESHOLD),
      b_async_(false),
      str_server_ip_(str_ip),
      i16_server_port_(i16_port),
      i32_worker_num_(1) {
}

int CthriftClient::InitWorker(bool async) {
  // key需要将过滤开关考虑进去
  string map_key =
      str_svr_appkey_ + str_serviceName_filter_ +
          boost::lexical_cast<std::string>(i32_port_filter_) + str_server_ip_
          + boost::lexical_cast<std::string>(i16_server_port_);
  // async模式下，必须开启并发模式；因为异步场景下thrift本身的sendBuf无法保证线程安全
  if (async) {
    b_parallel_ = true;
  }
  do {
    muduo::MutexLockGuard work_lock(work_resource_lock_);
    if (!b_parallel_ && map_appkey_worker_.count(map_key) >= i32_worker_num_) {
      std::pair<boost::unordered_multimap<string, WorkerWeakPtr>::iterator,
                boost::unordered_multimap<string, WorkerWeakPtr>::iterator>
          range = map_appkey_worker_.equal_range(map_key);
      for (boost::unordered_multimap<string, WorkerWeakPtr>::iterator
               it = range.first;
           it != range.second; ++it) {
        if (!(it->second).expired()) {
          sp_cthrift_client_worker_ = (it->second).lock();
          break;
        }
      }
    } else {
      sp_cthrift_client_worker_ = boost::make_shared<CthriftClientWorker>(
          str_svr_appkey_,
          str_cli_appkey_,
          str_serviceName_filter_,
          i32_port_filter_,
          str_server_ip_,
          i16_server_port_);

      if (async) {
        // startup async thread
        sp_cthrift_client_worker_->EnableAsync(i32_timeout_ms_);
      }

      map_appkey_worker_.insert(
          std::make_pair<string, WorkerWeakPtr>(map_key,
                                                sp_cthrift_client_worker_));
    }
  } while (0);

  Timestamp timestamp_start = Timestamp::now();

  muduo::Condition
      &cond = sp_cthrift_client_worker_->cond_avaliable_conn_ready();
  muduo::MutexLock &mutexlock =
      sp_cthrift_client_worker_->mutexlock_avaliable_conn_ready();

  const double
      d_default_timeout_secs = static_cast<double>(kI32DefaultTimeoutMsForClient) /
      MILLISENCOND_COUNT_IN_SENCOND;
  double d_left_time_sec = 0.0;
  bool b_timeout = false;

  while (0 >= sp_cthrift_client_worker_
      ->atomic_avaliable_conn_num()) {  // while, NOT if
    CTHRIFT_LOG_WARN("No good conn for appkey " << str_svr_appkey_
                                                << " from worker, wait");

    if (!CheckOverTime(timestamp_start,
                       d_default_timeout_secs,
                       &d_left_time_sec)) {
      do {
        muduo::MutexLockGuard lock(mutexlock);
        b_timeout = cond.waitForSeconds(d_left_time_sec);
      } while (0);

      if (b_timeout) {
        if (CTHRIFT_UNLIKELY(0 < sp_cthrift_client_worker_
            ->atomic_avaliable_conn_num())) {
          CTHRIFT_LOG_DEBUG("miss notify, but already get");
        } else {
          CTHRIFT_LOG_WARN("wait " << d_left_time_sec
                                   << " secs for good conn for "
                                   << "appkey " << str_svr_appkey_
                                   << " timeout, maybe need more time");
        }
        return SUCCESS;
      }

      if (CTHRIFT_UNLIKELY(CheckOverTime(timestamp_start,
                                         d_default_timeout_secs,
                                         0))) {
        CTHRIFT_LOG_WARN(d_default_timeout_secs << "secs countdown to 0, "
            "but no good conn ready, maybe need more time");
        return SUCCESS;
      }
    }
  }

  CTHRIFT_LOG_DEBUG("wait done, avaliable "
                        << "conn num " << sp_cthrift_client_worker_
      ->atomic_avaliable_conn_num());

// CLIENT_INIT(str_cli_appkey_, str_svr_appkey_); init cmtrace
  return SUCCESS;
}

int CthriftClient::Init(void) {
  boost::trim(str_svr_appkey_);
  if (str_svr_appkey_.empty() && (str_server_ip_.empty()
      || i16_server_port_ <= 0)) {
    CTHRIFT_LOG_ERROR("Fail to init cthrift client, the server appkey "
                          "can't be empty || ip port empty.");
    return ERR_PARA_INVALID;
  }

  int ret = g_cthrift_config.LoadConfig(true);
  if (ret != SUCCESS) {
    CTHRIFT_LOG_ERROR("Fail to load config. errno" <<  ret);
    return ret;
  }

  if (str_cli_appkey_.empty()) {
    str_cli_appkey_ = g_cthrift_config.client_appkey_;
  }

  if (str_server_ip_.empty()) {
    int ret = CthriftNameService::InitNS();
    if (0 != ret) {
      CTHRIFT_LOG_ERROR("init zk failed ");
      return ERR_NS_CON_NOT_READY;
    }
  }

  return InitWorker(b_async_);
}

int CthriftClient::SetFilterPort(const unsigned int &i32_port) {
  if (!ValidatePort(i32_port)) {
    CTHRIFT_LOG_ERROR("Fail to set the filter port, it can't be < 1 "
                          "|| > 65535. i32_port =" << i32_port);
    return ERR_PARA_INVALID;
  }
  i32_port_filter_ = i32_port;
  return SUCCESS;
}

int CthriftClient::SetFilterService(const std::string &str_serviceName) {
  string tmp = str_serviceName;
  boost::trim(tmp);
  if (tmp.empty()) {
    CTHRIFT_LOG_ERROR("The servicename can't be empty");
    return ERR_PARA_INVALID;
  }
  str_serviceName_filter_ = tmp;
  return SUCCESS;
}

int CthriftClient::SetClientAppkey(const std::string &str_appkey) {
  string tmp = str_appkey;
  boost::trim(tmp);
  if (tmp.empty()) {
    CTHRIFT_LOG_ERROR("The client appkey shouldn't be empty");
    return ERR_PARA_INVALID;
  } else {
    str_cli_appkey_ = tmp;
  }
  return SUCCESS;
}

boost::shared_ptr<TProtocol>
CthriftClient::GetCthriftProtocol(void) {
  CTHRIFT_LOG_INFO("cthrift transport init, thread info: "
                       << CurrentThread::tid());
  boost::shared_ptr<CthriftTransport> sp_cthrift_transport_
      = boost::make_shared<CthriftTransport>(
          str_svr_appkey_,
          i32_timeout_ms_,
          str_cli_appkey_,
          sp_cthrift_client_worker_);

  boost::shared_ptr<CthriftTBinaryProtoWithCthriftTrans>
      sp_cthrift_tbinary_protocol_ = boost::make_shared<
      CthriftTBinaryProtoWithCthriftTrans>(sp_cthrift_transport_,
                                           CTHRIFT_CLIENT);

  return sp_cthrift_tbinary_protocol_;
}
