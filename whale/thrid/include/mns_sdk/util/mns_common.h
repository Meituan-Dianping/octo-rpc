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

#ifndef OCTO_OPEN_SOURCE_UTIL_MNS_COMMON_H_
#define OCTO_OPEN_SOURCE_UTIL_MNS_COMMON_H_

#include <pthread.h>

#include <protocol/TBinaryProtocol.h>
#include <transport/TBufferTransports.h>
#include <transport/TSocket.h>
#include <Thrift.h>
#include <protocol/TProtocol.h>
#include <transport/TTransport.h>

#include <boost/make_shared.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/algorithm/string.hpp>
#include <boost/unordered/unordered_map.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/random.hpp>
#include <boost/bind.hpp>

#include <octoidl/naming_data_types.h>
#include <octoidl/naming_common_types.h>
#include <octoidl/naming_service_types.h>
#include <octoidl/ServiceAgent.h>

#include <rapidjson/document.h>
#include <rapidjson/writer.h>
#include <rapidjson/stringbuffer.h>

#include <muduo/base/CurrentThread.h>
#include <muduo/base/FileUtil.h>
#include <muduo/base/Mutex.h>
#include <muduo/base/CountDownLatch.h>
#include <muduo/base/ThreadLocalSingleton.h>
#include <muduo/base/ThreadPool.h>
#include <muduo/base/ThreadLocal.h>
#include <muduo/base/TimeZone.h>
#include <muduo/net/EventLoop.h>
#include <muduo/net/EventLoopThread.h>
#include <muduo/net/InetAddress.h>
#include <muduo/net/http/HttpContext.h>
#include <muduo/net/http/HttpResponse.h>
#include <muduo/net/TcpClient.h>
#include <muduo/net/TcpConnection.h>
#include <muduo/net/TimerId.h>
#include <muduo/base/Timestamp.h>

#include <vector>
#include <string>
#include <map>
#include <map>
#include <zlib.h>

#include "mns_log.h"

#define MNS_SAFE_DELETE(p) { if(p) { delete (p); (p)=NULL; } }
#define MNS_SAFE_FREE(p) { if(p) { free(p); (p)=NULL; } }
#define MNS_SAFE_DELETE_ARRAY(p) { if(p) { delete[] (p); (p)=NULL; } }
#define MNS_SAFE_RELEASE(p) { if(p) { (p)->Release(); (p)=NULL; } }

#define  kDRetryIntervalSec  10.0
#define  kI32DefaultTimeoutMS 5000
#define  kI32DefaultTimeoutForReuestMS 100

typedef enum {
  PROD, STAGING, DEV, PPE, TEST
} Appenv;

#endif  // OCTO_OPEN_SOURCE_UTIL_MNS_COMMON_H_
