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

using namespace apache::thrift::transport;
using namespace meituan_cthrift;

uint32_t CthriftTransport::read_virt(uint8_t *buf,
                                     uint32_t len) throw(TTransportException) {
  bool b_timeout;
  double d_left_secs = 0.0;

  while (1) {
    if (0 == ReadBufAvaliableReadSize()) {
      CTHRIFT_LOG_DEBUG("wait for read buf");
    } else {
      return ReadBufRead(buf, len);
    }

    if (!CheckOverTime(sp_shared_worker_transport_->timestamp_start,
                       static_cast<double>(
                           sp_shared_worker_transport_->i32_timeout_ms)
                           / MILLISENCOND_COUNT_IN_SENCOND, &d_left_secs)) {
      {
        muduo::MutexLockGuard lock(mutexlock_conn_ready);
        b_timeout =
            sp_shared_worker_transport_->p_cond_ready_read->waitForSeconds(
                d_left_secs);
      }

      if (b_timeout) {
        if (CTHRIFT_UNLIKELY(ReadBufAvaliableReadSize())) {
          CTHRIFT_LOG_DEBUG("miss notify, but buf already get");
          return ReadBufRead(buf, len);
        }

        break;
      }
    } else if (ReadBufAvaliableReadSize()) {  // check again for safe
      CTHRIFT_LOG_DEBUG("get read buf for appkey " << str_svr_appkey_
                                                   << " id "
                                                   << sp_shared_worker_transport_->str_id);
      // TODO(正常读取消息的场景下不用删除map中的context？)
      return ReadBufRead(buf, len);
    } else {
      break;
    }
  }

  CTHRIFT_LOG_WARN("wait appkey " << str_svr_appkey_ << " id " <<
                                  sp_shared_worker_transport_->str_id
                                  << " already "
                                  << sp_shared_worker_transport_->i32_timeout_ms
                                  << " ms for readbuf, timeout");

  sp_bool_timeout_.reset();  // clientworker will use weak_ptr to check timeout

  // if req NOT return, should del map here
  sp_cthrift_client_worker_->getP_event_loop_()->runInLoop(
      boost::bind(&CthriftClientWorker::DelContextMapByID,
                  sp_cthrift_client_worker_.get(),
                  sp_shared_worker_transport_->str_id));

  // worker no need to take the older task
  ResetWriteBuf();

  throw TTransportException(TTransportException::TIMED_OUT,
                            "wait for read buf timeout, maybe  server busy");
}

/*
uint32_t CthriftTransport::readEnd(void) {
  return ReadBufAvaliableReadSize();
}*/

void CthriftTransport::write_virt(const uint8_t *buf, uint32_t len) {
  AppendWriteBuf(buf, len);
}

void CthriftTransport::flush(void) throw(TTransportException) {
  sp_shared_worker_transport_->timestamp_start = Timestamp::now();
  //in case change timeout after init, so need match these two
  sp_shared_worker_transport_->i32_timeout_ms =
      i32_timeout_ms_;

  sp_bool_timeout_ = boost::make_shared<bool>();
  sp_shared_worker_transport_->wp_b_timeout = sp_bool_timeout_;

  // protocol already call SetID2Transport to set id

  CTHRIFT_LOG_DEBUG("appkey " << str_svr_appkey_ << " seqid "
                              << sp_shared_worker_transport_->str_id);

  muduo::Condition
      &cond = sp_cthrift_client_worker_->cond_avaliable_conn_ready();
  muduo::MutexLock &mutexlock =
      sp_cthrift_client_worker_->mutexlock_avaliable_conn_ready();

  bool b_timeout;
  double d_wait_secs = 0.0;
  const double d_timeout_secs = static_cast<double>(i32_timeout_ms_) /
      MILLISENCOND_COUNT_IN_SENCOND;

  while (0 >= sp_cthrift_client_worker_
      ->atomic_avaliable_conn_num()) {  // while, NOT if
    CTHRIFT_LOG_WARN("No good conn for appkey " << str_svr_appkey_
                                                << " from worker, wait");

    if (!CheckOverTime(sp_shared_worker_transport_->timestamp_start,
                       d_timeout_secs,
                       &d_wait_secs)) {
      do {
        muduo::MutexLockGuard lock(mutexlock);
        b_timeout = cond.waitForSeconds(d_wait_secs);
      } while (0);

      if (b_timeout) {
        if (CTHRIFT_UNLIKELY(0 < sp_cthrift_client_worker_
            ->atomic_avaliable_conn_num())) {
          CTHRIFT_LOG_DEBUG("miss notify, but already get avaliable conn");
        } else {
          CTHRIFT_LOG_ERROR("wait " << d_wait_secs
                                    << " secs for good conn timeout");

          throw TTransportException(TTransportException::TIMED_OUT,
                                    "wait for good conn timeout, maybe conn all "
                                        "be occupied or server list empty");
        }
      }

      if (CTHRIFT_UNLIKELY(CheckOverTime(
          sp_shared_worker_transport_->timestamp_start,
          d_timeout_secs, 0))) {
        CTHRIFT_LOG_WARN(i32_timeout_ms_
                             << "ms countdown to 0, "
                                 "but no good conn ready, maybe server busy");

        throw TTransportException(TTransportException::TIMED_OUT,
                                  "wait for good conn timeout, maybe conn "
                                      "all be occupied or server list empty");
      }
    }
  }

  // in case transport return,
  // then worker fill readbuf before erase id, for safe
  ResetReadBuf();

  /*boost::shared_ptr<muduo::net::EventLoopThread>
      sp_worker_thread = sp_cthrift_client_worker_->GetWorkerThreadSP();*/

  size_t sz_queue_size =
      sp_cthrift_client_worker_->getP_event_loop_()->queueSize();

  if (CTHRIFT_UNLIKELY(10 <= sz_queue_size)) {
    CTHRIFT_LOG_WARN("worker queue size " << sz_queue_size);
  } else {
    CTHRIFT_LOG_DEBUG("worker queue size " << sz_queue_size);
  }

  SharedContSharedPtr sp_shared = boost::make_shared<
      SharedBetweenWorkerTransport>(
      *sp_shared_worker_transport_);

  sp_cthrift_client_worker_->getP_event_loop_()->runInLoop(
      boost::bind(&CthriftClientWorker::SendTransportReq,
                  sp_cthrift_client_worker_.get(),
                  sp_shared));    // NOT sp_shared_worker_transport_ itself
}

// call by cthrift_protocol
void CthriftTransport::SetID2Transport(const std::string &str_id) {
  (sp_shared_worker_transport_->str_id).assign(str_id);
}

void CthriftTransport::ResetWriteBuf(void) {
  muduo::MutexLockGuard lock(*sp_mutexlock_write_buf);
  if (!(sp_write_tmembuf_.unique())) {
    // No need copy original buf since reset
    sp_write_tmembuf_ =
        boost::make_shared<TMemoryBuffer>();
  }

  sp_write_tmembuf_->resetBuffer();
}
