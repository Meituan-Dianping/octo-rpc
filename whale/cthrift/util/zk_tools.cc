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

#include "zk_tools.h"
#include "zk_client.h"

using namespace std;
using namespace meituan_cthrift;
using namespace ::apache::thrift::concurrency;

static const char ProtocolProviderThriftTail[] = "provider";
static const char ProtocolRouteThriftTail[] = "route";
static const char ProtocolProviderHttpTail[] = "provider-http";
static const char ProtocolRouteHttpTail[] = "route-http";

int ZkTools::InitZk(char *zk_host, int32_t timeout, bool retry) {
  int ret = ZkClient::GetInstance()->Init(zk_host, true, timeout, retry);
  if (MNS_SUCCESS != ret) {
    // ZK连接失败，直接报警，不退出sg_agent_worker
    CTHRIFT_LOG_ERROR(
        "ERR init mns processor failed! zk_host: " << zk_host
                                                   << ", timeout : " << timeout
                                                   << ", retry : " << retry
                                                   << ", ret: " << ret);
  }
  return ret;
}

int ZkTools::InvokeService(int service_name, void *service_params) {
  int ret = MNS_SUCCESS;
  timeval tvalStart;
  timeval tvalEnd;
  int64_t deltaTime;
  gettimeofday(&tvalStart, NULL);
  switch (service_name) {
    case ZK_GET: {
      ZkGetInvokeParams *req =
          static_cast<ZkGetInvokeParams *>(service_params);
      ret = ZkClient::GetInstance()->ZkGet(req);
      gettimeofday(&tvalEnd, NULL);
      deltaTime = ZkTools::DeltaTime(tvalEnd, tvalStart);
      CTHRIFT_LOG_DEBUG("zk get deltaTime: " << deltaTime);
      break;
    }
    case ZK_WGET: {
      ZkWGetInvokeParams *req =
          static_cast<ZkWGetInvokeParams *>(service_params);
      ret = ZkClient::GetInstance()->ZkWGet(req);
      CTHRIFT_LOG_DEBUG("Invoker wget_zk, serive_name: "
                            << service_name
                            << ", zk_path: "
                            << req->zk_wget_request.path);
      gettimeofday(&tvalEnd, NULL);
      deltaTime = ZkTools::DeltaTime(tvalEnd, tvalStart);
      CTHRIFT_LOG_DEBUG("zk wget deltaTime: " << deltaTime);
      break;
    }
    case ZK_WGET_CHILDREN: {
      ZkWGetChildrenInvokeParams *req =
          static_cast<ZkWGetChildrenInvokeParams *>(service_params);
      ret = ZkClient::GetInstance()->ZkWGetChildren(req);
      gettimeofday(&tvalEnd, NULL);
      deltaTime = ZkTools::DeltaTime(tvalEnd, tvalStart);
      CTHRIFT_LOG_DEBUG("zk wget children deltaTime: " << deltaTime);
      break;
    }
    default:break;
  }

  return ret;
}

int64_t ZkTools::DeltaTime(timeval end, timeval start) {
  return (end.tv_sec - start.tv_sec) * 1000000L
      + (end.tv_usec - start.tv_usec);
}

int ZkTools::SplitStringIntoVector(const char *sContent,
                                   const char *sDivider,
                                   std::vector<std::string> &vecStr) {
  char *sNewContent = new char[strlen(sContent) + 1];
  snprintf(sNewContent, strlen(sContent) + 1, "%s", sContent);
  char *pStart = sNewContent;

  std::string strContent;
  char *pEnd = strstr(sNewContent, sDivider);
  if (pEnd == NULL && strlen(sNewContent) > 0) {
    strContent = pStart;  // get the last one;
    vecStr.push_back(strContent);
  }

  while (pEnd) {
    *pEnd = '\0';
    strContent = pStart;
    vecStr.push_back(strContent);

    pStart = pEnd + strlen(sDivider);
    if ((*pStart) == '\0') {
      break;
    }

    pEnd = strstr(pStart, sDivider);

    if (pEnd == NULL) {
      strContent = pStart;  // get the last one;
      vecStr.push_back(strContent);
    }
  }

  SAFE_DELETE_ARRAY(sNewContent);
  return static_cast<int>(vecStr.size());
}

int ZkTools::GenProtocolZkPath(char (&zkPath)[MAX_BUF_SIZE],
                               const std::string &appkey,
                               const std::string &protocol,
                               const std::string &node_type) {
  int ret = snprintf(zkPath,
                     sizeof(zkPath),
                     "/octo/nameservice/%s/%s/%s",
                     g_cthrift_config.env_.c_str(),
                     appkey.c_str(),
                     node_type.c_str());
  if (ret <= 0) {
    CTHRIFT_LOG_ERROR("fail to generate "
                          << "appkey = " << appkey
                          << ", protocol = " << protocol
                          << ", nodeType = " << node_type);
    return FAILURE;
  }

  return MNS_SUCCESS;
}

int ZkTools::GenRegisterZkPath(char (&zkPath)[MAX_BUF_SIZE],
                               const std::string &appkey,
                               const std::string &protocol,
                               const int serverType) {
  // 获取nodeType
  std::string providerType = "provider";
  if ((!protocol.empty())) {
    if ("http" == protocol) {
      providerType += "-http";
    } else if ("thrift" == protocol) {
      CTHRIFT_LOG_DEBUG("thrift in newInterface: " << providerType);
    } else {
      providerType = providerType + "s/" + protocol;
    }
    CTHRIFT_LOG_INFO("provider in newInterface: " << providerType);
  } else {
    // 为了应对前端未修改protocol的情况
    if (HTTP_TYPE == serverType) {
      providerType += "-http";
    }
    CTHRIFT_LOG_DEBUG("provider in oldInterface: " << providerType);
  }
  CTHRIFT_LOG_INFO("zkPath provider prefix: " << providerType);

  // 拼接zkPath
  int ret =
      snprintf(zkPath,
               sizeof(zkPath),
               "/octo/nameservice/%s/%s/%s",
               g_cthrift_config.env_.c_str(),
               appkey.c_str(),
               providerType.c_str());
  if (ret <= 0) {
    CTHRIFT_LOG_ERROR("gen registerService fail! "
                          << "appkey = " << appkey
                          << ", protocol = " << protocol
                          << ", nodeType = " << providerType
                          << ", serverType = " << serverType);
    return -1;
  }

  return 0;
}

int ZkTools::DeGenNodeType(std::string *protocol) {
  if (protocol->empty()) {
    CTHRIFT_LOG_ERROR("protocol in DeGenNodeType is empty! ");
    return -1;
  }
  if (0 == protocol->compare(ProtocolProviderHttpTail)
      || 0 == protocol->compare(ProtocolRouteHttpTail)) {
    *protocol = "http";
  } else if (0 == protocol->compare(ProtocolProviderThriftTail)
      || 0 == protocol->compare(ProtocolRouteThriftTail)) {
    *protocol = "thrift";
  } else {
    CTHRIFT_LOG_INFO(
        "when watcher trigger, in DeGenNodeType, protocol:"
            << *protocol);
  }

  return 0;
}

int ZkTools::DeGenZkPath(const char *zkPath, std::string *appkey,
                         std::string *protocol) {
  std::vector<std::string> pathList;
  int ret = SplitStringIntoVector(zkPath, "/", pathList);
  if (0 < ret) {
    int length = static_cast<int>(pathList.size());
    // 这里必须保证zkPath符合 /octo/nameservice/环境/appkey/(nodeType + protocol)
    if (4 < length) {
      // appkey在数组的下标是4，是因为pathList[0]是""
      *appkey = pathList[4];
      if (appkey->empty()) {
        CTHRIFT_LOG_ERROR("DeGenZkPath appkey is empty! zkPath"
                              << zkPath);
        return -3;
      }
      // protocol取最后一个，有可能是cellar, provider, provider-http
      *protocol = pathList[length - 1];
      if (protocol->empty()) {
        CTHRIFT_LOG_ERROR("DeGenZkPath protocol is empty! zkPath"
                              << zkPath);
        return -3;
      }
    } else {
      CTHRIFT_LOG_ERROR("zkPath is not complete! "
                            << "zkPath: " << zkPath
                            << ", appkey: " << *appkey
                            << ", length: " << length);
      return -2;
    }
  } else {
    CTHRIFT_LOG_ERROR("zkPath is wroing! zkPath: " << zkPath << ","
        " appkey:" << *appkey);
    return -1;
  }

  /// Extract protocol from zkpath
  ret = ZkTools::DeGenNodeType(protocol);
  if (0 != ret) {
    CTHRIFT_LOG_ERROR("DeGenNodeType is wrong!  protocol:"
                          << *protocol);
  }

  return ret;
}

int ZkTools::SGService2Json(const meituan_mns::SGService &oservice,
                            std::string *strJson, const int env_int) {
  rapidjson::Document doc ;
  doc.Parse("{}");

  rapidjson::Document::AllocatorType& allocator = doc.GetAllocator();

  rapidjson::Value str_object_appkey(rapidjson::kStringType);
  str_object_appkey.SetString(rapidjson::StringRef(oservice.appkey.c_str()));
  doc.AddMember("appkey", str_object_appkey, allocator);

  rapidjson::Value str_object_version(rapidjson::kStringType);
  str_object_version.SetString(rapidjson::StringRef(oservice.version.c_str()));
  doc.AddMember("version", str_object_version, allocator);

  rapidjson::Value str_object_ip(rapidjson::kStringType);
  str_object_ip.SetString(rapidjson::StringRef(oservice.ip.c_str()));
  doc.AddMember("ip", str_object_ip, allocator);

  rapidjson::Value str_object_port(rapidjson::kNumberType);
  str_object_port.SetInt(oservice.port);
  doc.AddMember(rapidjson::StringRef("port"), str_object_port, allocator);

  rapidjson::Value str_object_weight(rapidjson::kNumberType);
  str_object_weight.SetInt(oservice.weight);
  doc.AddMember(rapidjson::StringRef("weight"), str_object_weight, allocator);

  rapidjson::Value str_object_status(rapidjson::kNumberType);
  str_object_status.SetInt(oservice.status);
  doc.AddMember(rapidjson::StringRef("status"), str_object_status, allocator);

  rapidjson::Value str_object_role(rapidjson::kNumberType);
  str_object_role.SetInt(oservice.role);
  doc.AddMember(rapidjson::StringRef("role"), str_object_role, allocator);

  rapidjson::Value str_object_env(rapidjson::kNumberType);
  str_object_env.SetInt(oservice.envir);
  doc.AddMember(rapidjson::StringRef("env"), str_object_env, allocator);

  //just adapt for java
  rapidjson::Value str_object_warmup(rapidjson::kNumberType);
  str_object_warmup.SetInt(0);
  doc.AddMember(rapidjson::StringRef("warmup"), str_object_warmup, allocator);

  rapidjson::Value str_object_lastUpdateTime(rapidjson::kNumberType);
  str_object_lastUpdateTime.SetInt(oservice.lastUpdateTime);
  doc.AddMember(rapidjson::StringRef("lastUpdateTime"), str_object_lastUpdateTime, allocator);

  rapidjson::Value str_object_fweight(rapidjson::kNumberType);
  str_object_fweight.SetDouble(oservice.fweight);
  doc.AddMember(rapidjson::StringRef("fweight"), str_object_fweight, allocator);

  rapidjson::Value str_object_serverType(rapidjson::kNumberType);
  str_object_serverType.SetInt(oservice.serverType);
  doc.AddMember(rapidjson::StringRef("serverType"), str_object_serverType, allocator);

  rapidjson::Value str_object_heartbeatSupport(rapidjson::kNumberType);
  str_object_heartbeatSupport.SetInt(oservice.heartbeatSupport);
  doc.AddMember(rapidjson::StringRef("heartbeatSupport"), str_object_heartbeatSupport, allocator);

  rapidjson::Value str_object_protocol(rapidjson::kStringType);
  str_object_protocol.SetString(rapidjson::StringRef(oservice.protocol.c_str
      ()));
  doc.AddMember("protocol", str_object_protocol, allocator);

  rapidjson::Value ObjectArray(rapidjson::kObjectType);
  for (std::map<std::string, meituan_mns::ServiceDetail>::const_iterator iter =
      oservice.serviceInfo.begin();
       iter != oservice.serviceInfo.end(); ++iter) {

    rapidjson::Value obj(rapidjson::kObjectType);
    obj.AddMember(rapidjson::StringRef("unifiedProto"), (iter->second
                      .unifiedProto ? 1 : 0),
                  allocator);
    ObjectArray.AddMember(rapidjson::StringRef(iter->first.c_str()), obj, allocator);
  }
  doc.AddMember("serviceInfo", ObjectArray, allocator);

  rapidjson::StringBuffer  buffer;
  rapidjson::Writer<rapidjson::StringBuffer>  writer(buffer);
  doc.Accept(writer);
  *strJson = buffer.GetString();

  CTHRIFT_LOG_INFO("out: " << *strJson);
  return MNS_SUCCESS;
}

int ZkTools::Json2SGService(const std::string &strJson,
                            meituan_mns::SGService *oservice) {
  rapidjson::Document doc;

  if(doc.Parse(strJson.c_str()).HasParseError()){
    return ERR_JSON_TO_DATA_FAIL;
  }

  if(doc.HasMember("appkey") && doc["appkey"].IsString()) {
    oservice->appkey = doc["appkey"].GetString();
  }

  if(doc.HasMember("version") && doc["version"].IsString()) {
    oservice->version = doc["version"].GetString();
  }

  if(doc.HasMember("ip") && doc["ip"].IsString()) {
    oservice->ip = doc["ip"].GetString();
  }

  if(doc.HasMember("port") && doc["port"].IsInt()) {
    oservice->port = doc["port"].GetInt();
  }

  if(doc.HasMember("weight") && doc["weight"].IsInt()) {
    oservice->weight = doc["weight"].GetInt();
  }

  if(doc.HasMember("status") && doc["status"].IsInt()) {
    oservice->status = doc["status"].GetInt();
  }

  if(doc.HasMember("role") && doc["role"].IsInt()) {
    oservice->role = doc["role"].GetInt();
  }

  if(doc.HasMember("env") && doc["env"].IsInt()) {
    oservice->envir = doc["env"].GetInt();
  }

  if(doc.HasMember("lastUpdateTime") && doc["lastUpdateTime"].IsInt()) {
    oservice->lastUpdateTime = doc["lastUpdateTime"].GetInt();
  }


  if(doc.HasMember("heartbeatSupport") && doc["heartbeatSupport"].IsInt()) {
    oservice->heartbeatSupport = static_cast<int8_t>(doc["heartbeatSupport"]
        .GetInt());
  }

  if(doc.HasMember("fweight") && doc["fweight"].IsDouble()) {
    oservice->fweight = doc["fweight"].GetDouble();
  }


  if(doc.HasMember("serverType") && doc["serverType"].IsInt()) {
    oservice->serverType = doc["serverType"].GetInt();
  }


  if(doc.HasMember("protocol") && doc["protocol"].IsString()) {
    oservice->protocol = doc["protocol"].GetString();
  }


  if(doc.HasMember("serviceInfo") && doc["serviceInfo"].IsObject()) {
    const rapidjson::Value &object = doc["serviceInfo"];

    for (rapidjson::Value::ConstMemberIterator iter = object.MemberBegin();
         iter != object.MemberEnd(); ++iter) {
      string name = (iter->name).GetString();
      const rapidjson::Value &value = iter->value;
      bool unifiedProto = false;

      if (value.HasMember("unifiedProto") && value["unifiedProto"].IsInt()) {
        unifiedProto = (0 != value["unifiedProto"].GetInt());
      }

      meituan_mns::ServiceDetail srv;
      srv.unifiedProto = unifiedProto;
      oservice->serviceInfo[name] = srv;
    }
  }
  return 0;
}

int ZkTools::ProviderNode2Json(const meituan_mns::CProviderNode &oprovider,
                               std::string *strJson) {
  rapidjson::Document doc ;
  doc.Parse("{}");

  rapidjson::Document::AllocatorType& allocator = doc.GetAllocator();

  rapidjson::Value str_object_appkey(rapidjson::kStringType);
  str_object_appkey.SetString(rapidjson::StringRef(oprovider.appkey.c_str()));
  doc.AddMember("appkey", str_object_appkey, allocator);


  rapidjson::Value str_object_time(rapidjson::kNumberType);
  str_object_time.SetDouble(static_cast<double>(oprovider.lastModifiedTime));
  doc.AddMember(rapidjson::StringRef("lastUpdateTime"), str_object_time, allocator);

  rapidjson::StringBuffer  buffer;
  rapidjson::Writer<rapidjson::StringBuffer>  writer(buffer);
  doc.Accept(writer);
  *strJson = buffer.GetString();
  return 0;
}
