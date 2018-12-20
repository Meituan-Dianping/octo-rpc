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

#ifndef OCTO_OPEN_SOURCE_MNS_SDK_SRC_UTIL_MNS_CONFIG_H_
#define OCTO_OPEN_SOURCE_MNS_SDK_SRC_UTIL_MNS_CONFIG_H_

#include "mns_sdk_common.h"


namespace mns_sdk {

enum ONOFFLINE {
  ONLINE,
  OFFLINE,
};

class MnsConfig {
 public:
  MnsConfig();
  int LoadConfig(const std::string &str_file_path);

 public:
  int InitAppnev(const std::map<std::string, std::string> &config_map);

  void GetHostIPInfo();

  std::string str_env_;
  std::string str_octo_env_;
  int enum_onoffline_env_;

  std::string url_;
  std::string chrion_appkey_;
  std::string chrion_sentienl_appkey_;
  std::string str_idc_path_;
  std::string str_local_chrion_port_;
  int local_chrion_port_;

  bool b_use_remote_;

  std::string str_local_ip_;
  std::string str_hostname_;
  std::string str_host_;

  std::string str_sentinel_http_request_;
};

extern mns_sdk::MnsConfig g_config;
}  // namespace mns_sdk




#endif  // OCTO_OPEN_SOURCE_MNS_SDK_SRC_UTIL_MNS_CONFIG_H_
