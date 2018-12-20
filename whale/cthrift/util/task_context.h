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

#ifndef CTHRIFT_SRC_CTHRIFT_UTIL_TASK_CONTEXT_H_
#define CTHRIFT_SRC_CTHRIFT_UTIL_TASK_CONTEXT_H_

#include "cthrift_common.h"

namespace meituan_cthrift {

static const int64_t kSleepInterval = 2 * 1000;  // us

template<class Request, class Response>
class TaskContext {
 public:
  explicit TaskContext(const Request &req)
      : req_(req), result_ready_(false) {}
  ~TaskContext() {}

  // 单位us
  void WaitResult(int64_t timeout) {
    int64_t time_spent = 0;

    while (true) {
      if (result_ready_ || time_spent > timeout) {
        break;
      }
      time_spent += kSleepInterval;
      usleep(kSleepInterval);
    }
  }

  void set_response(const Response &rsp) {
    rsp_ = rsp;
    result_ready_ = true;
  }

  const Request *get_request() const {
    return &req_;
  }

  const Response *get_response() const {
    if (result_ready_) {
      return &rsp_;
    }
    return NULL;
  }

 private:
  Request req_;
  Response rsp_;
  volatile bool result_ready_;
};

}  // namespace meituan_cthrift

#endif  // CTHRIFT_SRC_CTHRIFT_UTIL_TASK_CONTEXT_H_
