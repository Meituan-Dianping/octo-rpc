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


#ifndef CTHRIFT_SRC_CTHRIFT_UTIL_CTHRIFT_MNS_IMP_H_
#define CTHRIFT_SRC_CTHRIFT_UTIL_CTHRIFT_MNS_IMP_H_

#include "cthrift_ns_interface.h"

namespace meituan_cthrift {

class CthriftMnsImp {
 public:
  CthriftMnsImp();
  int32_t Init();
  void Destroy();

  virtual ~CthriftMnsImp();

  int32_t GetSrvList(ServicePtr service, const int64_t rcv_watcher_time = -1);

  int32_t RegisterService(const meituan_mns::SGService &oservice,
                          meituan_mns::RegistCmd::type regCmd = meituan_mns::RegistCmd::REGIST,
                          int uptCmd = meituan_mns::UptCmd::RESET);

};

extern meituan_cthrift::CthriftMnsImp g_cthrift_mns_default;

}  // namespace meituan_cthrift


#endif  // CTHRIFT_SRC_CTHRIFT_UTIL_CTHRIFT_NS_IMP_H_
