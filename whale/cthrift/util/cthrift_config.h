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

#ifndef CTHRIFT_SRC_CTHRIFT_UTIL_CTHRIFT_CONFIG_H_
#define CTHRIFT_SRC_CTHRIFT_UTIL_CTHRIFT_CONFIG_H_

#include <string>

namespace meituan_cthrift {

class CthriftConfig {
 public:
  CthriftConfig();
  int LoadConfig(const bool client = false);

 public:
  std::string ns_origin_;
  std::string server_version_;
  int listen_port_;
  std::string server_appkey_;
  std::string client_appkey_;
  int server_work_threadnum_;
  int server_conn_threadnum_;
  int server_max_connnum_;
  int server_timeout_;
  int server_conn_gctime_;
  bool server_register_;
  bool mns_origin_;
  std::string env_;
};

extern meituan_cthrift::CthriftConfig g_cthrift_config;
}  // namespace meituan_cthrift



#endif  // CTHRIFT_SRC_CTHRIFT_UTIL_CTHRIFT_CONFIG_H_
