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


#ifndef CTHRIFT_SRC_CTHRIFT_CTHRIFT_NAME_SERVICE_H_
#define CTHRIFT_SRC_CTHRIFT_CTHRIFT_NAME_SERVICE_H_

#include "cthrift/util/cthrift_common.h"
#include "cthrift/util/cthrift_ns_interface.h"

namespace meituan_cthrift {
using namespace apache::thrift;
using namespace apache::thrift::protocol;
using namespace apache::thrift::transport;

class CthriftNameService {
 public:
  static bool b_is_init_ns_;
  static muduo::MutexLock s_lock;

  static void IntranetIp(char ip[INET_ADDRSTRLEN]);
  static void GetHostIPInfo(void);  // fill ip, isMac, host,hostname

  static const double kDGetSvrListIntervalSecs;
  static const double kDFirstRegionMin;
  static const double kDSecondRegionMin;

  static std::string str_env_;
  static std::string str_swimlane_;
  static std::string str_local_ip_;
  static std::string str_host_;
  static std::string str_hostname_;

  static CthriftNsInterface ns_interface_;

  CthriftNameService(void) throw(TException);
  static void PackDefaultSgservice(const std::string &str_svr_appkey,
                                   const std::string &str_local_ip,
                                   const uint16_t &u16_port,
                                   meituan_mns::SGService *p_sgservice);

  static std::string SGService2String(
      const meituan_mns::SGService &sgservice);

  static double
  FetchOctoWeight(const double &fweight, const double &weight);

  static int32_t InitNS();
  static void UnInitNS();

  static int GetSrvListFrom(ServicePtr service);

  static int RegisterService(const meituan_mns::SGService &oservice);
};

extern const meituan_cthrift::CthriftNameService g_cthrift_ns;
}  // namespace meituan_cthrift


#endif  // CTHRIFT_SRC_CTHRIFT_CTHRIFT_NAME_SERVICE_H_
