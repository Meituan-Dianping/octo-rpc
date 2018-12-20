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

#include "cthrift_config.h"
#include "cthrift_common.h"

using namespace meituan_cthrift;

CthriftConfig meituan_cthrift::g_cthrift_config;

CthriftConfig::CthriftConfig() :
    ns_origin_(""),
    server_version_(DEFAULT_CONFIG_FRAMEWORK_VERSION),
    listen_port_(DEFAULT_CONFIG_SERVER_PORT),
    server_appkey_(DEFAULT_CONFIG_SERVER_APPKEY),
    client_appkey_(DEFAULT_CONFIG_CLIENT_APPKEY),
    server_work_threadnum_(DEFAULT_CONFIG_WORK_THREADNUM),
    server_conn_threadnum_(DEFAULT_CONFIG_CONN_THREADNUM),
    server_max_connnum_(DEFAULT_CONFIG_MAX_CONN_NUM),
    server_timeout_(DEFAULT_CONFIG_SERVER_TIMEOUT),
    server_conn_gctime_(DEFAULT_CONFIG_CONN_GC_TIME),
    server_register_(false),mns_origin_(false) {
}

int CthriftConfig::LoadConfig(const bool client) {
  std::ifstream in("conf.json");

  if (!in.is_open()) {
    CTHRIFT_LOG_ERROR("conf.json empty && need one config named config"
                          ".json and content:\r\n"<< "{\"ns\": {\"origin\":"
        " \"127.0.0.1:2188\",\"ismns\": 0,\"env\":\"test\" },"
        "\"client.Appkey\": \"com.sankuai.inf.newct.client\","
        "\"server.Version\": \"3.0.0\",\"server.ListenPort\": 16888,"
        "\"server.Appkey\": \"com.sankuai.inf.newct\",\"server.register\": 1,"
        "\"server.WorkThreadNum\": 4,\"server.MaxConnNum\": 10000,"
        "\"server.ServerTimeOut\": 100,\"server.ConnGCTime\": 10,"
        "\"server.ConnThreadNum\": 4 }");

    return ERR_EMPRY_CONFIG;
  }

  std::string str_conf_str((std::istreambuf_iterator<char>(in)),
                           std::istreambuf_iterator<char>());

  in.close();

  rapidjson::Document doc;
  if(doc.Parse(str_conf_str.c_str()).HasParseError()){
    return ERR_WRONG_CONTENT;
  }

  if(!client){
    if(doc.HasMember("server.register") && doc["server.register"].IsInt()) {
      int temp = doc["server.register"].GetInt();
      if (temp != 0) {
        server_register_ = true;
      }
    }
  }

  if(client || server_register_){

    if(doc.HasMember("ns") && doc["ns"].IsObject()) {
      const rapidjson::Value &ns = doc["ns"];

      if(ns.HasMember("origin") && ns["origin"].IsString()) {
        ns_origin_ = ns["origin"].GetString();
      }

      if(ns.HasMember("env") && ns["env"].IsString()) {
        env_ = ns["env"].GetString();
      }

      if(ns.HasMember("ismns") && ns["ismns"].IsInt()) {
        mns_origin_ = ns["ismns"].GetInt() == 1 ? true : false;
      }else{
        CTHRIFT_LOG_ERROR("conf.json  client or server_register_ but empty ns"
                              " items");
        return ERR_EMPTY_NS_ORIGIN;
      }
    }
    else {
      CTHRIFT_LOG_ERROR("conf.json  client or server_register_ but empty ns"
                            " items");
      return ERR_EMPTY_NS_ORIGIN;
    }
  }

  if (client) {
    if(doc.HasMember("client.Appkey") && doc["client.Appkey"].IsString()) {
      server_version_ = doc["client.Appkey"].GetString();
    }

    CTHRIFT_LOG_DEBUG("client just init : MnsHost");
    return SUCCESS;
  }

  if(doc.HasMember("server.Version") && doc["server.Version"].IsString()) {
    server_version_ = doc["server.Version"].GetString();
  }

  if(doc.HasMember("server.ListenPort") && doc["server.ListenPort"].IsInt()) {
    listen_port_ = doc["server.ListenPort"].GetInt();
  }


  if(doc.HasMember("server.Appkey") && doc["server.Appkey"].IsString()) {
    server_appkey_ = doc["server.Appkey"].GetString();
  }

  if(doc.HasMember("server.WorkThreadNum") && doc["server.WorkThreadNum"].IsInt()) {
    server_work_threadnum_ = doc["server.WorkThreadNum"].GetInt();
  }


  if(doc.HasMember("server.ConnThreadNum") && doc["server.ConnThreadNum"].IsInt()) {
    server_conn_threadnum_ = doc["server.ConnThreadNum"].GetInt();
  }

  if(doc.HasMember("server.MaxConnNum") && doc["server.MaxConnNum"].IsInt()) {
    server_max_connnum_ = doc["server.MaxConnNum"].GetInt();
  }


  if(doc.HasMember("server.ServerTimeOut") && doc["server.ServerTimeOut"].IsInt()) {
    server_timeout_ = doc["server.ServerTimeOut"].GetInt();
  }

  if(doc.HasMember("server.ConnGCTime") && doc["server.ConnGCTime"].IsInt()) {
    server_conn_gctime_ = doc["server.ConnGCTime"].GetInt();
  }

  return SUCCESS;
}