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

#ifndef CTHRIFT_SRC_CTHRIFT_UTIL_ZK_TOOLS_H_
#define CTHRIFT_SRC_CTHRIFT_UTIL_ZK_TOOLS_H_

#include "cthrift_common.h"

namespace meituan_cthrift {

typedef struct ZkGetRequest {
  int zk_index;
  std::string path;
  int watch;
  ZkGetRequest() : zk_index(0), path(""), watch(0) {}
} ZkGetRequest;

typedef struct ZkGetResponse {
  std::string buffer;
  int buffer_len;
  struct Stat stat;
  int err_code;
} ZkGetResponse;

typedef struct ZkGetInvokeParams {
  ZkGetRequest zk_get_request;
  ZkGetResponse zk_get_response;
} ZkGetInvokeParams;

typedef struct ZkWGetRequest {
  std::string path;
  watcher_fn watch;
  void *watcherCtx;
} ZkWGetRequest;

typedef struct ZkWGetResponse {
  std::string buffer;
  int buffer_len;
  struct Stat stat;
  int err_code;
} ZkWGetResponse;

typedef struct ZkWGetInvokeParams {
  ZkWGetRequest zk_wget_request;
  ZkWGetResponse zk_wget_response;
} ZkWGetInvokeParams;

typedef struct ZkWGetChildrenRequest {
  std::string path;
  watcher_fn watch;
  void *watcherCtx;
} ZkWGetChildrenRequest;

typedef struct ZkWGetChildrenResponse {
  int err_code;
  int count;
  std::vector<std::string> data;
} ZkWGetChildrenResponse;

typedef struct ZkWGetChildrenInvokeParams {
  ZkWGetChildrenRequest zk_wgetchildren_request;
  ZkWGetChildrenResponse zk_wgetchildren_response;
} ZkWGetChildrenInvokeParams;

typedef struct ZkCreateRequest {
  std::string path;
  std::string value;
  bool  ephemeral;
  int value_len;
} ZkCreateRequest;

typedef struct ZkCreateInvokeParams {
  ZkCreateRequest zk_create_request;
  int zk_create_response;
} ZkCreateInvokeParams;

typedef struct ZkSetRequest {
  std::string path;
  std::string buffer;
  int version;
} ZkSetRequest;

typedef struct ZkExistsRequest {
  std::string path;
  int watch;
} ZkExistsRequest;

enum ServiceNameType {
  ZK_GET = 1,
  ZK_WGET,
  ZK_WGET_CHILDREN,
};

class ZkTools {
 public:
  static int InitZk(char *zk_host, int32_t timeout, bool retry);

  static int64_t DeltaTime(timeval end, timeval start);

  static int SplitStringIntoVector(const char *sContent,
                                   const char *sDivider,
                                   std::vector<std::string> &vecStr);

  static int InvokeService(int service_name, void *service_params);

  static int GenRegisterZkPath(char (&zkPath)[MAX_BUF_SIZE],
                               const std::string &appkey,
                               const std::string &protocol,
                               const int serverType);

  static int GenProtocolZkPath(char (&zkPath)[MAX_BUF_SIZE],
                               const std::string &appkey,
                               const std::string &protocol,
                               const std::string &node_type);

  // 根据传入zkPath，解析出appkey, protocol
  static int DeGenZkPath(const char *zkPath, std::string *appkey,
                         std::string *protocol);

  static int DeGenNodeType(std::string *protocol);

  static int Json2SGService(const std::string &strJson,
                            meituan_mns::SGService *oservice);

  static int SGService2Json(const meituan_mns::SGService &oservice,
                            std::string *strJson, const int env_int);

  static int ProviderNode2Json(const meituan_mns::CProviderNode &oprovider,
                               std::string *strJson);
};
}  // namespace meituan_cthrift

#endif  // CTHRIFT_SRC_CTHRIFT_UTIL_ZK_TOOLS_H_
