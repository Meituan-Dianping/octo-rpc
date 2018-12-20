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


#include "cthrift_name_service.h"
#include "cthrift/util/zk_client.h"
#include "cthrift/util/cthrift_ns_imp.h"
#include "cthrift/util/cthrift_mns_imp.h"

using namespace std;
using namespace meituan_cthrift;

const double CthriftNameService::kDGetSvrListIntervalSecs = 10.0;
const double CthriftNameService::kDFirstRegionMin = 1.0;
const double CthriftNameService::kDSecondRegionMin = 0.001;

string CthriftNameService::str_env_;
string CthriftNameService::str_swimlane_;
string CthriftNameService::str_local_ip_;
string CthriftNameService::str_host_;
string CthriftNameService::str_hostname_;

bool CthriftNameService::b_is_init_ns_ = false;

muduo::MutexLock CthriftNameService::s_lock;

const CthriftNameService g_cthrift_ns;

CthriftNsInterface  CthriftNameService::ns_interface_;

CthriftNameService::CthriftNameService(void) throw
(TException) {
  GetHostIPInfo();  // fill str_local_ip_, b_isMac_, str_host_
}

void CthriftNameService::PackDefaultSgservice(
    const string &str_svr_appkey,
    const string &str_local_ip,
    const uint16_t &u16_port,
    meituan_mns::SGService *p_sgservice) {
  p_sgservice->__set_appkey(str_svr_appkey);
  p_sgservice->__set_version(g_cthrift_config.server_version_);
  p_sgservice->__set_ip(str_local_ip);
  p_sgservice->__set_port(u16_port);
  p_sgservice->__set_weight(10);

  p_sgservice->__set_status(meituan_mns::fb_status::ALIVE);

  p_sgservice->__set_lastUpdateTime(static_cast<int32_t>(time(0)));
  p_sgservice->__set_fweight(10.0);
  p_sgservice->__set_serverType(0);
  p_sgservice->__set_protocol("thrift");
  p_sgservice->__set_heartbeatSupport(0);
}

string CthriftNameService::SGService2String(
    const meituan_mns::SGService &sgservice) {
  string str_ret
      ("appkey:" + sgservice.appkey + " version:" + sgservice.version + " ip:"
           + sgservice.ip);

  try {
    str_ret.append(" port:" + boost::lexical_cast<string>(sgservice.port));
  } catch (boost::bad_lexical_cast &e) {

    CTHRIFT_LOG_ERROR("boost::bad_lexical_cast :" << e.what()
                                                  << "sgservice.port : "
                                                  << sgservice.port);
  }

  try {
    str_ret.append(
        " weight:" + boost::lexical_cast<string>(sgservice.weight));
  } catch (boost::bad_lexical_cast &e) {

    CTHRIFT_LOG_ERROR("boost::bad_lexical_cast :" << e.what()
                                                  << "sgservice.weight : "
                                                  << sgservice.weight);
  }

  try {
    str_ret.append(
        " status:" + boost::lexical_cast<string>(sgservice.status));
  } catch (boost::bad_lexical_cast &e) {

    CTHRIFT_LOG_ERROR("boost::bad_lexical_cast :" << e.what()
                                                  << "sgservice.status : "
                                                  << sgservice.status);
  }

  try {
    str_ret.append(" role:" + boost::lexical_cast<string>(sgservice.role));
  } catch (boost::bad_lexical_cast &e) {

    CTHRIFT_LOG_ERROR("boost::bad_lexical_cast :" << e.what()
                                                  << "sgservice.role : "
                                                  << sgservice.role);
  }

  try {
    str_ret.append(
        " envir:" + boost::lexical_cast<string>(sgservice.envir));
  } catch (boost::bad_lexical_cast &e) {

    CTHRIFT_LOG_ERROR("boost::bad_lexical_cast :" << e.what()
                                                  << "sgservice.envir : "
                                                  << sgservice.envir);
  }

  try {
    str_ret.append(
        " fweight:" + boost::lexical_cast<string>(sgservice.fweight));
  } catch (boost::bad_lexical_cast &e) {

    CTHRIFT_LOG_ERROR("boost::bad_lexical_cast :" << e.what()
                                                  << "sgservice.fweight : "
                                                  << sgservice.fweight);
  }

  try {
    str_ret.append(" serverType:"
                       + boost::lexical_cast<string>(sgservice.serverType));
  } catch (boost::bad_lexical_cast &e) {

    CTHRIFT_LOG_ERROR("boost::bad_lexical_cast :" << e.what()
                                                  << "sgservice.serverType : "
                                                  << sgservice.serverType);
  }
  return str_ret;
}

void CthriftNameService::IntranetIp(char ip[INET_ADDRSTRLEN]) {
  struct ifaddrs *ifAddrStruct = NULL;
  struct ifaddrs *ifa = NULL;
  void *tmpAddrPtr = NULL;
  int addrArrayLen = 32;
  char addrArray[addrArrayLen][INET_ADDRSTRLEN];
  getifaddrs(&ifAddrStruct);
  int index = 0;
  for (ifa = ifAddrStruct; ifa != NULL; ifa = ifa->ifa_next) {
    if (!ifa->ifa_addr) {
      continue;
    }
    if (0 == strcmp(ifa->ifa_name, "vnic"))
      continue;
    if (ifa->ifa_addr->sa_family == AF_INET) {  // check it is IP4
      // tmpAddrPtr = &((struct sockaddr_in *) ifa->ifa_addr)->sin_addr;
      tmpAddrPtr =
          &(reinterpret_cast<struct sockaddr_in *>(ifa->ifa_addr))->sin_addr;
      inet_ntop(AF_INET, tmpAddrPtr, addrArray[index], INET_ADDRSTRLEN);
      if (0 == strcmp(addrArray[index], "127.0.0.1"))
        continue;
      strcpy(ip, addrArray[index]);
      if (++index >= addrArrayLen - 1)
        break;
    }
  }
  if (index > 1) {
    int idx = 0;
    while (idx < index) {
      if (NULL != strstr(addrArray[idx], "10.")
          && 0 == strcmp(addrArray[idx], strstr(addrArray[idx], "10."))) {
        strcpy(ip, addrArray[idx]);
      }
      idx++;
    }
  }
  if (ifAddrStruct != NULL)
    freeifaddrs(ifAddrStruct);
  return;
}

// fill ip, host, hostname
void CthriftNameService::GetHostIPInfo(void) {
  char ip[INET_ADDRSTRLEN] = {0};

  IntranetIp(ip);
  if (CTHRIFT_UNLIKELY(0 == strlen(ip))) {
    muduo::CurrentThread::sleepUsec(MILLISENCOND_COUNT_IN_SENCOND);

    IntranetIp(ip);
  }

  if (CTHRIFT_UNLIKELY(0 == strlen(ip))) {
    str_local_ip_.assign("127.0.0.1");
  } else {
    str_local_ip_.assign(ip);
  }

  char hostCMD[TEMP_BUFFER_LENGTH] = {0};
  strncpy(hostCMD, "host ", 5);
  strncpy(hostCMD + 5, ip, INET_ADDRSTRLEN);

  FILE *fp = popen(hostCMD, "r");
  char hostInfo[TEMP_BUFFER_LENGTH] = {0};

  if (CTHRIFT_LIKELY(!fgets(hostInfo, TEMP_BUFFER_LENGTH, fp))) {
    int iRet = ferror(fp);
    if (CTHRIFT_UNLIKELY(iRet)) {
      pclose(fp);
      return;
    }
  }
  hostInfo[strlen(hostInfo) - 1] = '\0';  // del line token

  str_host_.assign(hostInfo);
  str_host_.assign(StrToLower(str_host_));
  ReplaceAllDistinct(" ", "%20", &str_host_);

  pclose(fp);

  char hostnameCMD[TEMP_BUFFER_LENGTH] = {0};

  strncpy(hostnameCMD, "hostname ", 9);
  fp = popen(hostnameCMD, "r");
  char hostname[TEMP_BUFFER_LENGTH] = {0};
  if (CTHRIFT_LIKELY(!fgets(hostname, TEMP_BUFFER_LENGTH, fp))) {
    int iRet = ferror(fp);
    if (CTHRIFT_UNLIKELY(iRet)) {
      pclose(fp);
      return;
    }
  }

  hostname[strlen(hostname) - 1] = '\0';  // del line token

  str_hostname_.assign(hostname);
  str_hostname_.assign(StrToLower(str_hostname_));
  ReplaceAllDistinct(" ", "%20", &str_hostname_);
  pclose(fp);
}

double CthriftNameService::FetchOctoWeight(const double &fweight,
                                           const double &weight) {
  return (!CheckDoubleEqual(fweight, weight)
      && !CheckDoubleEqual(fweight, static_cast<double>(0))) ? fweight : weight;
}

int CthriftNameService::GetSrvListFrom(ServicePtr service) {
  if (!b_is_init_ns_) {
    CTHRIFT_LOG_ERROR("GetSrvListFrom b_is_init_ns_ false");
    return -1;
  }

  return ns_interface_.GetSrvList(service);
}

int CthriftNameService::RegisterService(
    const meituan_mns::SGService &oservice) {
  if (!b_is_init_ns_) {
    CTHRIFT_LOG_ERROR("GetSrvListFrom b_is_init_ns_ false");
    return -1;
  }

  return ns_interface_.RegisterService(oservice);
}

int32_t CthriftNameService::InitNS() {
  if (b_is_init_ns_) {
    CTHRIFT_LOG_DEBUG("InitNS already SUCCESS");
    return 0;
  }

  muduo::MutexLockGuard lock(s_lock);

  if (b_is_init_ns_) {
    CTHRIFT_LOG_DEBUG("InitNS already SUCCESS");
    return 0;
  }

  CTHRIFT_LOG_INFO("InitNS BEGIN");

  if(g_cthrift_config.mns_origin_){
    // 用户可以设置自己的注册及获取服务列表函数 桥接模式
    ns_interface_.SetDesFunction(
        boost::bind(&CthriftMnsImp::Destroy, &g_cthrift_mns_default));
    ns_interface_.SetGetFunction(
        boost::bind(&CthriftMnsImp::GetSrvList, &g_cthrift_mns_default, _1, _2));
    ns_interface_.SetInitFunction(
        boost::bind(&CthriftMnsImp::Init, &g_cthrift_mns_default));
    ns_interface_.SetRegFunction(
        boost::bind(&CthriftMnsImp::RegisterService, &g_cthrift_mns_default,
                    _1, _2, _3));
    CTHRIFT_LOG_INFO("----------ns use mns------------------");
  }else{
    // 用户可以设置自己的注册及获取服务列表函数 桥接模式
    ns_interface_.SetDesFunction(
        boost::bind(&CthriftNsImp::Destroy, &g_cthrift_ns_default));
    ns_interface_.SetGetFunction(
        boost::bind(&CthriftNsImp::GetSrvList, &g_cthrift_ns_default, _1, _2));
    ns_interface_.SetInitFunction(
        boost::bind(&CthriftNsImp::Init, &g_cthrift_ns_default));
    ns_interface_.SetRegFunction(
        boost::bind(&CthriftNsImp::RegisterService, &g_cthrift_ns_default,
                    _1, _2, _3));
    CTHRIFT_LOG_INFO("----------ns use zk------------------");
  }

  if (ns_interface_.Init() != 0) {
    CTHRIFT_LOG_ERROR("InitNS failed");
    return -1;
  }

  b_is_init_ns_ = true;
  CTHRIFT_LOG_INFO("InitNS END");
  return 0;
}

void CthriftNameService::UnInitNS() {
  if (!b_is_init_ns_) {
    CTHRIFT_LOG_DEBUG("InitNS have no init");
  }

  muduo::MutexLockGuard lock(s_lock);

  if (!b_is_init_ns_) {
    CTHRIFT_LOG_DEBUG("InitNS already uinit");
  }

  ns_interface_.Destory();

  b_is_init_ns_ = false;
}
