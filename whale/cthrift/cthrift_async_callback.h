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

#ifndef CTHRIFT_SRC_CTHRIFT_ASYNC_CALLBACK_H_
#define CTHRIFT_SRC_CTHRIFT_ASYNC_CALLBACK_H_

#include "cthrift/util/cthrift_common.h"

namespace meituan_cthrift {

template<class CobClient>
class AsyncCallback {
 public:
  // typedef void (*AsyncCobType) (CobClient *client);
  typedef boost::function<void(CobClient *client)> AsyncCobType;

  AsyncCallback() : cob_success_(boost::bind(&Default, _1)),
                    cob_timeout_(boost::bind(&Default, _1)),
                    cob_highwater_(boost::bind(&Default, _1)) {
  }

  ~AsyncCallback() {
  }

  static void Default(CobClient *client) {
    CTHRIFT_LOG_DEBUG("Default callback function");
  }

  void Callback(CobClient *client) {
    AsyncState async_task_state = boost::any_cast<AsyncState>(
        (muduo::net::EventLoop::getEventLoopOfCurrentThread())
            ->getContext());
    switch (async_task_state) {
      case kTaskStateSuccess : CTHRIFT_LOG_DEBUG("Call Success cob_: ");
        cob_success_(client);
        break;
      case kTaskStateTimeOut : CTHRIFT_LOG_DEBUG("Call Timeout cob_: ");
        cob_timeout_(client);
        break;
      case kTaskStateTooMany : CTHRIFT_LOG_DEBUG("Call HighWater cob_: ");
        cob_highwater_(client);
        break;
      default: CTHRIFT_LOG_ERROR("Invalid callback type");
    }
  }

  void Success(const AsyncCobType &cob) {
    cob_success_ = cob;
  }
  void Timeout(const AsyncCobType &cob) {
    cob_timeout_ = cob;
  }
  void HighWater(const AsyncCobType &cob) {
    cob_highwater_ = cob;
  }

 private:
  AsyncCobType cob_success_;
  AsyncCobType cob_timeout_;
  AsyncCobType cob_highwater_;
};
}  // namespace meituan_cthrift

#endif  // CTHRIFT_SRC_CTHRIFT_ASYNC_CALLBACK_H_
