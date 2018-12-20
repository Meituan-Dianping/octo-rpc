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

#include "cthrift_ns_imp.h"
#include "zk_client.h"

using namespace meituan_cthrift;

CthriftNsImp meituan_cthrift::g_cthrift_ns_default;

int32_t CthriftNsImp::Init() {
  int ret = ZkTools::InitZk(
      const_cast<char *>(g_cthrift_config.ns_origin_.c_str()),
      ZK_TIMEOUT_SENCOND, true);
  if (0 != ret) {
    CTHRIFT_LOG_ERROR("InitZk failed.");
    return -1;
  }

  if (!p_m_zk_client) {
    p_m_zk_client = new ServiceZkClient(this);
  }

  return 0;
}

void CthriftNsImp::Destroy() {
  if (p_m_zk_client) {
    delete p_m_zk_client;
  }
}

CthriftNsImp::CthriftNsImp() : retry_(ZK_DEFAULT_RETRY_TIMES),
                               p_m_zk_client(NULL) {
}

CthriftNsImp::~CthriftNsImp() {
}

int32_t CthriftNsImp::GetSrvList(ServicePtr service,
                                 const int64_t rcv_watcher_time) {
  std::string local_appkey = service->localAppkey;
  std::string remote_appkey = service->remoteAppkey;
  std::string version = service->version;
  std::string protocol = service->protocol;
  bool is_watcher_callback = -1 != rcv_watcher_time ? true : false;

  if (remote_appkey.empty()) {
    CTHRIFT_LOG_ERROR("remote_appkey cannot be empty.");
    return FAILURE;
  }

  std::vector<meituan_mns::SGService> serviceList;
  // get From zk
  int ret = p_m_zk_client->GetSrvListByProtocol(&serviceList,
                                                local_appkey,
                                                remote_appkey,
                                                protocol,
                                                is_watcher_callback);

  if (ret == ERR_ZK_LIST_SAME_BUFFER) {
    // already log inside
    return -3;
  }

  if (ERR_NODE_NOTFIND == ret) {
    CTHRIFT_LOG_WARN("can not find service node from zk"
                         << ", appkey = " << remote_appkey
                         << ", protocol = " << protocol
                         << ", current version = " << version);
    // return empty list
    serviceList.clear();
    return MNS_SUCCESS;
  }

  if (MNS_SUCCESS == ret) {
    CTHRIFT_LOG_DEBUG("succeed to get service list from zk"
                          << ", remoteAppkey = " << remote_appkey
                          << ", protocol = " << protocol
                          << ", serviceList' size = " << serviceList.size());
    service->__set_version(version);
    service->__set_serviceList(serviceList);
    return MNS_SUCCESS;
  }

  CTHRIFT_LOG_ERROR("getServiceList from zk fail, "
                        << ", remoteAppkey = " << remote_appkey
                        << ", protocol = " << protocol
                        << ", serviceList' size = " << serviceList.size());
  return -4;
}

int32_t CthriftNsImp::RegisterService(
    const meituan_mns::SGService &oservice,
    meituan_mns::RegistCmd::type regCmd, int32_t uptCmd) {
  // 比较重要的服务，日志级别提高
  std::string serviceName = "";
  std::string unifiedProto = "";
  for (std::map<std::string, meituan_mns::ServiceDetail>::const_iterator iter
      = oservice.serviceInfo.begin();
       iter != oservice.serviceInfo.end(); ++iter) {
    serviceName += iter->first + " ";
    unifiedProto += iter->second.unifiedProto + " ";
  }
  CTHRIFT_LOG_INFO("to register service to ZK"
                       << ", appkey : " << oservice.appkey
                       << ", ip : " << oservice.ip
                       << ", version : " << oservice.version
                       << ", port : " << oservice.port
                       << ", env : " << oservice.envir
                       << ", status : " << oservice.status
                       << "; fweight : " << oservice.fweight
                       << "; protocol: " << oservice.protocol
                       << "; serverType : " << oservice.serverType
                       << "; serviceName : " << serviceName
                       << "; unifiedProto : " << unifiedProto
                       << "; regCmd : " << regCmd);

  int loop_times = 0;
  do {
    int ret = RegisterServiceNodeToZk(oservice, regCmd, uptCmd);
    if (ret == 0 || ERR_NODE_NOTFIND == ret) {
      return ret;
    } else {
      CTHRIFT_LOG_INFO("retry to registry , appkey is : "
                           << oservice.appkey);

      // 超过最大重试次数
      if (loop_times > retry_) {
        CTHRIFT_LOG_ERROR("register service to ZK fail! ""loop_times > retry_,"
                              << "loop_times is : " << loop_times
                              << ", appkey : " << oservice.appkey.c_str()
                              << ", ip : " << oservice.ip
                              << ", port : " << oservice.port
                              << ", env : " << oservice.envir
                              << ", status : " << oservice.status
                              << "; fweight : " << oservice.fweight
                              << "; serverType : " << oservice.serverType
                              << "; protocol: " << oservice.protocol
                              << ", default retry times is : " << retry_);
        return ERR_REGIST_SERVICE_ZK_FAIL;
      }
    }
  } while (retry_ > ++loop_times);

  return MNS_SUCCESS;
}

int32_t CthriftNsImp::RegisterServiceNodeToZk(
    const meituan_mns::SGService &oservice,
    meituan_mns::RegistCmd::type regCmd, int32_t uptCmd) {

  char zkProviderPath[MAX_BUF_SIZE] = {0};
  int ret =
      ZkTools::GenRegisterZkPath(zkProviderPath, oservice.appkey,
                                 oservice.protocol, oservice.serverType);
  if (0 != ret) {
    CTHRIFT_LOG_ERROR("_gen registerService fail! "
                          << "appkey = " << oservice.appkey
                          << ", protocol = " << oservice.protocol
                          << ", serverType = " << oservice.serverType);
    return ret;
  }
  CTHRIFT_LOG_INFO("_gen registerService zkProviderPath: " << zkProviderPath);

  // Firstly, check appkey node.
  // Dont create one. Just let service owner register it by MSGP.
  boost::shared_ptr<ZkExistsRequest> zk_exists_req =
      boost::shared_ptr<ZkExistsRequest>(new ZkExistsRequest());
  zk_exists_req->path = zkProviderPath;
  zk_exists_req->watch = 0;
  ret = ZkClient::GetInstance()->ZkExists(zk_exists_req.get());
  if (ZNONODE == ret || ret == -1) {
    CTHRIFT_LOG_ERROR("appkey not exist."
                          << "zkpath:" << zkProviderPath
                          << ", ip : " << oservice.ip
                          << ", port : " << oservice.port
                          << "; fweight : " << oservice.fweight
                          << "; serverType : " << oservice.serverType
                          << "; protocol: " << oservice.protocol
                          << ", or zk handle fail ret: " << ret);

    ZkCreateRequest zk_create_req;
    ZkCreateInvokeParams zk_create_params;
    zk_create_req.path = zkProviderPath;
    zk_create_req.ephemeral = false;
    zk_create_req.value = "";
    zk_create_req.value_len = 0;
    zk_create_params.zk_create_request = zk_create_req;
    int create_ret = ZkClient::GetInstance()->ZkCreate(&zk_create_params);
    if (ZOK != create_ret || create_ret == -1) {
      CTHRIFT_LOG_WARN("WARN zoo_create failed "
                           << "zkPath:" << zkProviderPath
                           << ", ret:" << create_ret);
      return create_ret;
    }
  }

  // Check service node, create or update it
  char zkPath[MAX_BUF_SIZE] = {0};
  snprintf(zkPath, sizeof(zkPath), "%s/%s:%d", zkProviderPath,
           oservice.ip.c_str(), oservice.port);

  std::string strJson;
  zk_exists_req->path = zkPath;
  zk_exists_req->watch = 0;
  ret = ZkClient::GetInstance()->ZkExists(zk_exists_req.get());
  if (ZNONODE == ret) {
    if (meituan_mns::RegistCmd::UNREGIST == regCmd || meituan_mns::UptCmd::DELETE == uptCmd) {
      // if the zk node don't exist, ignore unRegister
      CTHRIFT_LOG_WARN(
          "ignore unRegister, because the zk node don't exist"
              << ", zkPath: " << zkPath
              << ", status : " << oservice.status
              << "; fweight : " << oservice.fweight
              << "; serverType : " << oservice.serverType
              << "; protocol : " << oservice.protocol
              << ", ip : " << oservice.ip
              << ", regCmd: " << regCmd
              << ", uptCmd: " << uptCmd);
      return ERR_NODE_NOTFIND;
    }

    meituan_mns::SGService oTmp
        = const_cast<meituan_mns::SGService &>(oservice);
    // todo for env
    ZkTools::SGService2Json(oTmp, &strJson, 0);

    CTHRIFT_LOG_INFO("register: create a new zk node"
                         << ", zkPath: " << zkPath
                         << ", appkey : " << oTmp.appkey
                         << ", env : " << oTmp.envir
                         << ", status : " << oTmp.status
                         << "; fweight : " << oTmp.fweight
                         << "; serverType : " << oTmp.serverType
                         << "; protocol : " << oTmp.protocol
                         << ", ip : " << oTmp.ip);
    ZkCreateRequest zk_create_req;
    ZkCreateInvokeParams zk_create_params;
    zk_create_req.path = zkPath;
    zk_create_req.ephemeral = true;
    zk_create_req.value = strJson;
    zk_create_req.value_len = static_cast<int>(strJson.size());
    zk_create_params.zk_create_request = zk_create_req;
    ret = ZkClient::GetInstance()->ZkCreate(&zk_create_params);
    if (ZOK != ret || ret == -1) {
      CTHRIFT_LOG_WARN("WARN zoo_create failed "
                           << "zkPath:" << zkPath
                           << ", zValue:" << strJson.c_str()
                           << ", zValue size:" << strJson.size()
                           << ", ret:" << ret);
      return ret;
    }
  } else if (ZOK == ret) {
    std::string strOrgJson;
    struct Stat stat;

    ZkGetRequest zk_get_req;
    ZkGetInvokeParams zk_get_param;
    zk_get_req.path = zkPath;
    zk_get_req.watch = 0;
    zk_get_param.zk_get_request = zk_get_req;
    ret = ZkClient::GetInstance()->ZkGet(&zk_get_param);
    if (ZOK != ret) {
      CTHRIFT_LOG_WARN("zoo_get origin content fail or zk handle is null,"
                           << " ret: " << ret << ", zkPath: " << zkPath);
      return ret;
    }
    strOrgJson = zk_get_param.zk_get_response.buffer;
    stat = zk_get_param.zk_get_response.stat;

    meituan_mns::SGService orgService;
    ret = ZkTools::Json2SGService(strOrgJson, &orgService);
    if (ret != 0) {
      CTHRIFT_LOG_ERROR("Json2cthrift::SGService failed! "
                            "strJson = " << strOrgJson
                                         << "ret = " << ret);
      return ret;
    }

    if (meituan_mns::fb_status::STOPPED == orgService.status ||
        meituan_mns::fb_status::STARTING == orgService.status) {
      std::string status_str =
          meituan_mns::fb_status::STOPPED == orgService.status ? "STOPPED" : "STARTING";
      CTHRIFT_LOG_INFO("the zk node "
                           << "status is " << status_str
                           << ", don't change its status, "
                           << "appkey = " << oservice.appkey
                           << ", ip = " << oservice.ip
                           << ", port = " << oservice.port);
    } else {
      CTHRIFT_LOG_INFO("the zk node "
                           << "status(" << orgService.status
                           << ") is not equals to the one which is "
                           << "defined by user, use the later("
                           << oservice.status << ")");
      orgService.status = oservice.status;
    }
    // reset oService last_update_time
    orgService.lastUpdateTime = static_cast<int32_t>(time(0));

    if (meituan_mns::RegistCmd::REGIST == regCmd) {
      orgService.version = oservice.version;

      orgService.heartbeatSupport = oservice.heartbeatSupport;
      EditServiceName(orgService, oservice, uptCmd);
    }

    strOrgJson = "";
    // NOTICE: sg2Service always return 0 need todo for env
    if (ZkTools::SGService2Json(orgService, &strOrgJson, 0) < 0) {
      CTHRIFT_LOG_ERROR("SGService2Json failed");
      return -1;
    }

    CTHRIFT_LOG_INFO("to regist when node exists"
                         << ", uptCmd : " << uptCmd
                         << ", appkey : " << orgService.appkey
                         << ", env : " << orgService.envir
                         << ", status : " << orgService.status
                         << "; fweight : " << orgService.fweight
                         << "; serverType : " << orgService.serverType
                         << "; protocol: " << orgService.protocol
                         << ", ip : " << orgService.ip);
    boost::shared_ptr<ZkSetRequest> zk_set_req =
        boost::shared_ptr<ZkSetRequest>(new ZkSetRequest());
    zk_set_req->path = zkPath;
    zk_set_req->buffer = strOrgJson;
    zk_set_req->version = stat.version;
    ret = ZkClient::GetInstance()->ZkSet(zk_set_req.get());
    if (ZOK != ret || ret == -1) {
      CTHRIFT_LOG_ERROR("ERR zoo_set failed "
                            << "zkPath:" << zkPath
                            << " szValue:" << strOrgJson.c_str()
                            << " szValue size:" << strOrgJson.size()
                            << "ret:" << ret);
      return ret;
    }
  } else if (ZNOAUTH == ret) {
    CTHRIFT_LOG_ERROR("ZNOAUTH in registerService. ret = " << ret);
    return ret;
  } else {
    CTHRIFT_LOG_ERROR("ERR other error: " << ret << " in registerService");
    return ret;
  }

  // update service node last modified time
  meituan_mns::CProviderNode oprovider;
  oprovider.appkey = oservice.appkey;
  oprovider.lastModifiedTime = time(NULL);

  strJson = "";
  ZkTools::ProviderNode2Json(oprovider, &strJson);
  boost::shared_ptr<ZkSetRequest> zk_set_req =
      boost::shared_ptr<ZkSetRequest>(new ZkSetRequest());
  zk_set_req->path = zkProviderPath;
  zk_set_req->buffer = strJson;
  zk_set_req->version = -1;
  ret = ZkClient::GetInstance()->ZkSet(zk_set_req.get());
  if (ZOK != ret || FAILURE == ret) {
    CTHRIFT_LOG_ERROR("ERR zoo_set provider failed "
                          "zkPath:" << zkProviderPath
                                    << " szValue:" << strJson.c_str()
                                    << " szValue size:" << strJson.size()
                                    << "ret: " << ret);
    return ret;
  }

  return MNS_SUCCESS;
}

int32_t CthriftNsImp::EditServiceName(
    meituan_mns::SGService &desService,
    const meituan_mns::SGService &srcService,
    int32_t uptCmd) {
  switch (uptCmd) {
    case meituan_mns::UptCmd::RESET:desService.serviceInfo = srcService.serviceInfo;
      break;
    case meituan_mns::UptCmd::ADD:
      for (std::map<std::string, meituan_mns::ServiceDetail>::const_iterator iter =
          srcService.serviceInfo.begin();
           iter != srcService.serviceInfo.end();
           ++iter) {
        desService.serviceInfo[iter->first] = iter->second;
      }
      break;
    case meituan_mns::UptCmd::DELETE:
      for (std::map<std::string, meituan_mns::ServiceDetail>::const_iterator iter =
          srcService.serviceInfo.begin();
           iter != srcService.serviceInfo.end();
           ++iter) {
        desService.serviceInfo.erase(iter->first);
      }
      break;
    default: CTHRIFT_LOG_ERROR("unknown uptCmd: " << uptCmd);
      return -1;
  }
  return 0;
}
