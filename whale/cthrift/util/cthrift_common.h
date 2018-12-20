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

#ifndef CTHRIFT_SRC_CTHRIFT_UTIL_CTHRIFT_COMMON_H_
#define CTHRIFT_SRC_CTHRIFT_UTIL_CTHRIFT_COMMON_H_

#include <math.h>
#include <dlfcn.h>
#include <arpa/inet.h>
#include <ifaddrs.h>
#include <netinet/in.h>
#include <unistd.h>
#include <stdio.h>
#include <sys/time.h>
#include <sys/socket.h>
#include <stddef.h>
#include <errno.h>
#include <pthread.h>
#include <utility>
#include <sys/prctl.h>
#include <netdb.h>
#include <limits>

#include <boost/make_shared.hpp>
#include <boost/noncopyable.hpp>
#include <boost/unordered_set.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/bind.hpp>
#include <boost/shared_array.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/function.hpp>
#include <boost/algorithm/string.hpp>

#include <thrift/protocol/TBinaryProtocol.h>
#include <thrift/transport/TBufferTransports.h>
#include <thrift/transport/TTransportUtils.h>
#include <thrift/protocol/TProtocol.h>
#include <thrift/transport/TSocket.h>
#include <thrift/async/TAsyncChannel.h>

#include <muduo/base/AsyncLogging.h>
#include <muduo/base/CurrentThread.h>
#include <muduo/base/Logging.h>
#include <muduo/base/FileUtil.h>
#include <muduo/base/Mutex.h>
#include <muduo/base/ThreadLocalSingleton.h>
#include <muduo/base/ThreadPool.h>
#include <muduo/base/ThreadLocal.h>
#include <muduo/base/TimeZone.h>
#include <muduo/net/EventLoop.h>
#include <muduo/net/EventLoopThread.h>
#include <muduo/net/EventLoopThreadPool.h>
#include <muduo/net/InetAddress.h>
#include <muduo/net/TcpClient.h>
#include <muduo/net/TcpConnection.h>
#include <muduo/net/TcpServer.h>
#include <muduo/net/TimerId.h>
#include <muduo/base/Timestamp.h>
#include <muduo/net/Endian.h>
#include <muduo/base/CountDownLatch.h>
#include <muduo/base/Exception.h>

#include <thrift/Thrift.h>
#include <thrift/server/TServer.h>
#include <thrift/protocol/TProtocol.h>
#include <thrift/protocol/TVirtualProtocol.h>

#include <concurrency/PosixThreadFactory.h>

#include <sstream>
#include <iostream>
#include <fstream>
#include <queue>
#include <string>
#include <vector>
#include <map>
#include <set>
#include <list>


#include <rapidjson/document.h>
#include <rapidjson/writer.h>
#include <rapidjson/stringbuffer.h>

#include "log4cplus.h"
#include "cthrift_config.h"

#include <mns_sdk/mns_sdk.h>
#include <octoidl/naming_data_types.h>
#include <octoidl/naming_common_types.h>

extern "C" {
#ifdef htonl
#undef htonl
#endif
#ifdef htonll
#undef htonll
#endif
#include <zookeeper/zookeeper.h>
}


namespace meituan_cthrift {
using apache::thrift::transport::TMemoryBuffer;
using apache::thrift::transport::TTransport;

using namespace muduo;

typedef boost::shared_ptr<muduo::net::TcpClient> TcpClientSharedPtr;
typedef boost::weak_ptr<muduo::net::TcpClient> TcpClientWeakPtr;
typedef boost::shared_ptr<TMemoryBuffer> TMemBufSharedPtr;

const int MNS_SUCCESS = 0;
const int FAILURE = -1;
const int ZK_CLIENT_TIMEOUT = 10000;
const int ZK_RETRY = 3;
const int DEFAULT_SERVICE_TIMEOUT = 200000;

const int ERR_NODE_NOTFIND = -101;
const int ERR_INVALID_PARAM = 400;
const int ERR_FAILEDSENDMSG = -220044;
const int ERR_JSON_TO_DATA_FAIL = -300012;

// -3 01 xxx for zk
const int ERR_ZK_LIST_SAME_BUFFER = -301001;
const int ERR_GET_ZK_HANDLER_FAIL = -301002;
const int ERR_ZK_CONNECTION_LOSS = -301003;
const int ERR_ZK_EVENTLOOP_TIMEOUT = -301004;

// -3 02 xxx for serviceList
const int ERR_REGIST_SERVICE_ZK_FAIL = -302001;
const int ERR_NODE_LOST = -302002;

const int MAX_BUF_SIZE = 1024;
const int kZkContentSize = 1024;
const int32_t kMaxZkPathDepth = 8;

const int THRIFT_TYPE = 0;
const int HTTP_TYPE = 1;

#define SAFE_DELETE(p) { if (p) { delete (p); (p)=NULL; } }
#define SAFE_FREE(p) { if (p) { free(p); (p)=NULL; } }
#define SAFE_DELETE_ARRAY(p) { if (p) { delete[] (p); (p)=NULL; } }
#define SAFE_RELEASE(p) { if (p) { (p)->Release(); (p)=NULL; } }

#define SLEEP_FOR_SAFE_EXIT 1000*1000
#define MILLISENCOND_COUNT_IN_SENCOND 1000
#define SENCOND_COUNT_IN_MIN 60
#define DEFAULT_ASYNC_PENDING_THRESHOLD 30
#define WORKER_THREAD_POS_SECOND 60*5
#define TEMP_BUFFER_LENGTH 256
#define ZK_TIMEOUT_SENCOND 10
#define ZK_DEFAULT_RETRY_TIMES 3
#define ZK_DEFAULT_RETRY_SLEEP_US 50*1000
#define MAX_ZK_THREADPOOL_NUM 20
#define MIX_ZK_THREADPOOL_NUM 0

#define KD_RETRY_INTERVAL_SEC 10.0

#define DEFAULT_CONFIG_SERVER_APPKEY "com.company.newct"
#define DEFAULT_CONFIG_FRAMEWORK_VERSION "3.0.0"
#define DEFAULT_CONFIG_CLIENT_APPKEY "com.company.newct.client"

#define DEFAULT_CONFIG_SERVER_PORT 16888
#define DEFAULT_CONFIG_CONN_THREADNUM 4
#define DEFAULT_CONFIG_WORK_THREADNUM 40
#define DEFAULT_CONFIG_MAX_CONN_NUM 10000
#define DEFAULT_CONFIG_SERVER_TIMEOUT 30
#define DEFAULT_CONFIG_CONN_GC_TIME 600

#define CTHRIFT_LIKELY(x)  (__builtin_expect(!!(x), 1))
#define CTHRIFT_UNLIKELY(x)  (__builtin_expect(!!(x), 0))
#define BYTE unsigned char

enum RegionType {
  kRegionTypeOne,
  kRegionTypeTwo,
  kRegionTypeThree,
};

enum State {
  kExpectFrameSize,
  kExpectFrame
};

enum AsyncState {
  kTaskStateInit = 0,
  kTaskStateSuccess,
  kTaskStateTimeOut,
  kTaskStateTooMany
};

enum ErrorNum {
  SUCCESS = 0,
  ERR_EMPRY_CONFIG = 1,
  ERR_WRONG_CONTENT = 2,
  ERR_EMPTY_NS_ORIGIN =3,
  ERR_EMPTY_ENV = 4,
  ERR_CON_NOT_READY = 5,
  ERR_NS_CON_NOT_READY = 6,
  ERR_PARA_INVALID = 7,
  ERR_INVALID_PORT = 8,
  ERR_INVALID_TIMEOUT = 9,
  ERR_INVALID_MAX_CONNNUM = 10,
};


int16_t NumCPU(void);

bool CheckDoubleEqual(const double &d1, const double &d2);

void ReplaceAllDistinct
    (const std::string &old_value,
     const std::string &new_value,
     std::string *p_str);

bool CheckOverTime(const muduo::Timestamp &timestamp, const double
&d_overtime_secs, double *p_d_left_secs);

bool ValidatePort(const unsigned int &port);

int32_t GetStringLimit();

std::string StrToLower(const std::string &str_tmp);
}  // namespace meituan_cthrift

extern const int16_t kI16CpuNum;

extern muduo::AtomicInt32 g_atomic_i32_seq_id;

#endif  // CTHRIFT_SRC_CTHRIFT_UTIL_CTHRIFT_COMMON_H_

