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


#ifndef CTHRIFT_SRC_CTHRIFT_UTIL_CTHRIFT_NS_INTERFACE_H_
#define CTHRIFT_SRC_CTHRIFT_UTIL_CTHRIFT_NS_INTERFACE_H_

#include <boost/bind.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/make_shared.hpp>

#include <pthread.h>
#include <muduo/base/Mutex.h>
#include <muduo/base/Atomic.h>

#include "cthrift_common.h"
#include "cthrift_zk_client.h"
#include "log4cplus.h"

namespace meituan_cthrift {

typedef boost::shared_ptr<meituan_mns::getservice_res_param_t> ServicePtr;
typedef boost::function<void()> DestroyFunction;
typedef boost::function<int()> InitFunction;
typedef boost::function<int(ServicePtr service,
                            const long rcv_watcher_time)> GetFunction;
typedef boost::function<int(const meituan_mns::SGService &oservice,
                            meituan_mns::RegistCmd::type regCmd,
                            int uptCmd)> RegFunction;

class CthriftNsInterface {
 public:
  void SetRegFunction(RegFunction reg) {
    reg_ = reg;
  }

  void SetInitFunction(InitFunction init) {
    init_ = init;
  }

  void SetDesFunction(DestroyFunction des) {
    destroy_ = des;
  }

  void SetGetFunction(GetFunction get) {
    get_ = get;
  }

  CthriftNsInterface() {
    init_ = boost::bind(&CthriftNsInterface::OnInit, this);
    destroy_ = boost::bind(&CthriftNsInterface::OnDestory, this);
    reg_ = boost::bind(&CthriftNsInterface::OnRegisterService,
                       this, _1, _2, _3);
    get_ = boost::bind(&CthriftNsInterface::OnGetSrvList,
                       this, _1, _2);
  }

  virtual ~CthriftNsInterface() {
  }

  int32_t Init() {
    return init_();
  }

  void Destory() {
    destroy_();
  }

  int32_t GetSrvList(ServicePtr service,
                     const int64_t rcv_watcher_time = -1) {
    return get_(service, rcv_watcher_time);
  }

  int32_t RegisterService(const meituan_mns::SGService &oservice,
                          meituan_mns::RegistCmd::type regCmd = meituan_mns::RegistCmd::REGIST,
                          int32_t uptCmd = meituan_mns::UptCmd::RESET) {
    return reg_(oservice, regCmd, uptCmd);
  }

 private:
  int32_t OnRegisterService(const meituan_mns::SGService &oservice,
                            meituan_mns::RegistCmd::type regCmd,
                            int32_t uptCmd) {
    CTHRIFT_LOG_WARN("OnRegisterService default.");
    return -1;
  }

  int32_t OnGetSrvList(ServicePtr service, const int64_t rcv_watcher_time) {
    CTHRIFT_LOG_WARN("OnGetSrvList default.");
    return -1;
  }

  void OnDestory() {
    CTHRIFT_LOG_WARN("OnDestory default.");
    return;
  }

  int32_t OnInit() {
    CTHRIFT_LOG_WARN("OnInit default.");
    return -1;
  }

  DestroyFunction destroy_;
  InitFunction init_;
  RegFunction reg_;
  GetFunction get_;
};

}  // namespace meituan_cthrift


#endif  // CTHRIFT_SRC_CTHRIFT_UTIL_CTHRIFT_NS_INTERFACE_H_
