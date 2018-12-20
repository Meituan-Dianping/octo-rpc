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

#include "cthrift_mns_imp.h"

using namespace meituan_cthrift;

CthriftMnsImp meituan_cthrift::g_cthrift_mns_default;

int32_t CthriftMnsImp::Init() {
  return  mns_sdk::InitMNS(g_cthrift_config.ns_origin_, 10);
}

void CthriftMnsImp::Destroy() {
  return  mns_sdk::DestroyMNS();
}

CthriftMnsImp::CthriftMnsImp() {
}

CthriftMnsImp::~CthriftMnsImp() {
}

int32_t CthriftMnsImp::GetSrvList(ServicePtr service,
                                 const int64_t rcv_watcher_time) {

  std::string local_appkey = service->localAppkey;
  std::string remote_appkey = service->remoteAppkey;
  std::string version = service->version;
  std::string protocol = service->protocol;

  std::vector<meituan_mns::SGService> svr_list;

  int ret =  mns_sdk::getSvrList(remote_appkey,
                                 local_appkey,
                                 protocol,
                                 "",
                                 &svr_list);


  if(ret != 0){
    CTHRIFT_LOG_WARN("failed to GetSrvList ret=" << ret);
    return ret;
  }


  service->__set_version("");
  service->__set_serviceList(svr_list);
  return 0;
}

int32_t CthriftMnsImp::RegisterService(
    const meituan_mns::SGService &oservice,
    meituan_mns::RegistCmd::type regCmd, int32_t uptCmd) {
  std::vector<string> service_list;
  for (std::map<std::string, meituan_mns::ServiceDetail>::const_iterator iter
      = oservice.serviceInfo.begin();
       iter != oservice.serviceInfo.end(); ++iter) {
    service_list.push_back(iter->first);
  }
  CTHRIFT_LOG_INFO("to register service to mns"
                       << ", appkey : " << oservice.appkey
                       << ", port : " << oservice.port
                       << "; protocol: " << oservice.protocol
                       << "; serverType : " << oservice.serverType);


 return mns_sdk::StartSvr(oservice.appkey,
                            service_list,
                            static_cast<int16_t>(oservice.port),
                            oservice.serverType,
                            oservice.protocol);
}
