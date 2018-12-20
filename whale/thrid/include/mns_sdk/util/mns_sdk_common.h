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

#ifndef OCTO_OPEN_SOURCE_MNS_SDK_COMMON_H_
#define OCTO_OPEN_SOURCE_MNS_SDK_COMMON_H_

#include "mns_common.h"
#include "mns_config.h"

namespace mns_sdk {
using namespace std;

#define MNS_LIKELY(x)  (__builtin_expect(!!(x), 1))
#define MNS_UNLIKELY(x)  (__builtin_expect(!!(x), 0))

#define ERR_CHECK_CONNECTION -300001
#define ERR_CREATE_CONNECTION -300002
#define ERR_CLOSE_CONNECTION -300003

#define CONNECTION_RETRY_TIMES        1
#define SG_SERVER_READ_TIMEOUT        100
#define SG_SERVER_CONN_TIMEOUT        50
#define SG_SERVER_WRITE_TIMEOUT       50
#define SG_LOCAL_SERVER_READ_TIMEOUT  30
#define SG_LOCAL_SERVER_CONN_TIMEOUT  10
#define SG_LOCAL_SERVER_WRITE_TIMEOUT 10

struct HttpContext {
  muduo::net::HttpContext http_context;
  uint32_t u32_want_len;
};
typedef boost::shared_ptr<HttpContext> HttpContextSharedPtr;

class MnsSdkCommon {
 public:
  MnsSdkCommon(void);

  static std::string SGService2String(const meituan_mns::SGService &sgservice);

  static int8_t ParseSentineSgagentList
      (const std::string &str_req,
       std::vector<meituan_mns::SGService> *p_vec_sgservice);

  static int Hex2Decimal(const char *begin, const char *end);
  static void ParseHttpChunkData(muduo::net::Buffer *pBuf,
                                 muduo::net::HttpContext *pContext);
  static bool ParseHttpRequest(uint32_t *pudwWantLen,
                               muduo::net::Buffer *buf,
                               muduo::net::HttpContext *context,
                               muduo::Timestamp receiveTime);

  static int FetchJsonValByKey4Doc(rapidjson::Document &reader,
                                   const std::string &strKey,
                                   rapidjson::Document::MemberIterator *pitr);

  static int FetchJsonValByKey4Val(rapidjson::Value &reader,
                                   const std::string &strKey,
                                   rapidjson::Value::MemberIterator *pitr);

  static void IntranetIp(char ip[INET_ADDRSTRLEN]);

  static int8_t FetchInt32FromJson
      (const std::string &strKey,
       rapidjson::Value &data_single,
       int32_t *p_i32_value);

  static void replace_all_distinct
      (const std::string &old_value,
       const std::string &new_value,
       std::string *p_str);

  static int CheckEmptyJsonStringVal(const
                                     rapidjson::Document::MemberIterator &itr);
  static int Httpgzdecompress(Byte *zdata, uLong nzdata,
                              Byte *data, uLong *ndata);

  static string strToLower(const string &str_tmp);

  static double
  FetchOctoWeight(const double &fweight, const double &weight);

  static bool CheckDoubleEqual(const double &d1, const double &d2);

  static void PackDefaultSgservice(const string &str_svr_appkey,
                                   const string &str_local_ip,
                                   const uint16_t &u16_port,
                                   meituan_mns::SGService *p_sgservice);

  static bool CheckOverTime(const muduo::Timestamp &timestamp,
                            const double &d_overtime_secs,
                            double *p_d_left_secs);

  const static double kDFirstRegionMin;
  const static double kDSecondRegionMin;
};

}

#endif //OCTO_OPEN_SOURCE_MNS_SDK_COMMON_H_
