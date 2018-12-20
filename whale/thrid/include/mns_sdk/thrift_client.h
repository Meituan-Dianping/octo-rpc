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

#ifndef OCTO_OPEN_SOURCE_THRIFT_CLIENT_H_
#define OCTO_OPEN_SOURCE_THRIFT_CLIENT_H_

#include "util/mns_sdk_common.h"

namespace mns_sdk {

enum ProcType {
  SG_AEGNT_TYPE,
  UNKNOW_TYPE,
};

class ThriftClientHandler {
 public:
  ThriftClientHandler();
  ~ThriftClientHandler();
  int init(const std::string &host, int port, ProcType proc_type, bool local
  = true);
  int checkConnection();
  int createConnection();
  int closeConnection();
  int checkHandler();
  void *getClient() {
    return m_client;
  }
  bool m_closed;
  std::string m_host;
  int m_port;
  void *m_client;
  boost::shared_ptr<apache::thrift::transport::TSocket> m_socket;
  boost::shared_ptr<apache::thrift::transport::TTransport> m_transport;
  boost::shared_ptr<apache::thrift::protocol::TProtocol> m_protocol;
  ProcType type;
};
};
#endif  // OCTO_OPEN_SOURCE_THRIFT_CLIENT_H_